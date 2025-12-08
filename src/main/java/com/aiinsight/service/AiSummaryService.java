package com.aiinsight.service;

import com.aiinsight.config.AiConfig;
import com.aiinsight.domain.article.NewsArticle;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import org.springframework.scheduling.annotation.Async;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiSummaryService {

    private final AiConfig aiConfig;
    private final NewsArticleService newsArticleService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String SUMMARY_PROMPT = """
        다음 AI 관련 뉴스 기사를 분석해주세요.

        중요: URL에 접근하거나 WebFetch 도구를 사용하지 마세요. 아래 제공된 정보만으로 분석해주세요.

        기사 제목: %s
        기사 내용: %s

        다음 형식의 JSON으로만 응답해주세요 (다른 텍스트 없이 JSON만):
        {
            "titleKo": "기사 제목을 자연스러운 한국어로 번역",
            "summary": "3-5문장으로 핵심 내용을 한국어로 요약 (제공된 정보 기반)",
            "relevanceScore": 0.0-1.0 사이의 AI 관련성 점수,
            "category": "LLM|COMPUTER_VISION|NLP|ROBOTICS|ML_OPS|RESEARCH|INDUSTRY|STARTUP|REGULATION|TUTORIAL|PRODUCT|OTHER 중 하나",
            "importance": "HIGH|MEDIUM|LOW 중 하나"
        }

        카테고리 설명:
        - LLM: 대규모 언어 모델 (GPT, Claude, Llama, Gemini 등)
        - COMPUTER_VISION: 이미지/영상 처리, 객체 인식
        - NLP: 자연어 처리 (번역, 감정 분석 등)
        - ROBOTICS: 로봇 공학, 자율주행
        - ML_OPS: 머신러닝 운영, 인프라
        - RESEARCH: 학술 연구, 논문
        - INDUSTRY: 산업 동향, 시장 분석
        - STARTUP: 스타트업 소식, 투자
        - REGULATION: 법규, 정책, 윤리
        - TUTORIAL: 튜토리얼, 가이드
        - PRODUCT: 신제품 출시, 업데이트
        - OTHER: 기타

        중요도 기준:
        - HIGH: 업계 전반에 영향을 미치는 중요한 뉴스 (GPT, Claude, Gemini 등 주요 모델 관련)
        - MEDIUM: 특정 분야에 유의미한 뉴스
        - LOW: 일반적인 소식

        필수 주의사항:
        - 외부 URL 접근 없이 제공된 제목과 내용만 사용
        - titleKo는 원문 제목을 자연스러운 한국어로 번역
        - summary는 한국어로 작성 (정보가 부족하면 제목 기반으로 추정)
        - 반드시 JSON 형식으로만 응답 (설명 텍스트 금지)
        """;

    public void summarizeArticle(NewsArticle article) {
        summarizeArticle(article, false);
    }

    /**
     * 비동기로 기사 요약 실행 (크롤링 응답을 블로킹하지 않음)
     * @param articleId 분석할 기사 ID
     */
    @Async("aiAnalysisExecutor")
    public void summarizeArticleAsync(Long articleId) {
        try {
            NewsArticle article = newsArticleService.findEntityById(articleId);
            if (article != null) {
                summarizeArticle(article, false);
            }
        } catch (Exception e) {
            log.error("비동기 AI 분석 실패 (기사 ID: {}): {}", articleId, e.getMessage());
        }
    }

    /**
     * 기사 요약 (강제 재분석 옵션 포함)
     * @param article 분석할 기사
     * @param forceReanalyze true인 경우 이미 분석된 기사도 재분석
     */
    public void summarizeArticle(NewsArticle article, boolean forceReanalyze) {
        if (!forceReanalyze && article.getIsSummarized()) {
            log.debug("이미 요약된 기사: {}", article.getId());
            return;
        }

        if (forceReanalyze) {
            log.info("강제 재분석 모드: 기사 {} 재분석 시작", article.getId());
        }

        String title = article.getTitle();
        String content = article.getContent();
        String url = article.getOriginalUrl();

        // 이미지/미디어 URL인 경우 분석 건너뛰기
        if (isMediaUrl(url)) {
            log.warn("미디어 파일 URL 분석 건너뜀 (기사 ID: {}): {}", article.getId(), url);
            return;
        }

        // 제목이 없거나 일반적인 제목인 경우 URL에서 정보 추출 시도
        boolean needMetadataFetch = (title == null || title.isEmpty() || isGenericTitle(title));
        String[] metadata = null;

        if (needMetadataFetch && url != null && !url.isEmpty()) {
            metadata = fetchMetadataFromUrl(url);
            if (metadata[0] != null && !metadata[0].isEmpty()) {
                // 원본 기사의 title도 업데이트
                article.setTitle(metadata[0]);
                title = metadata[0];
                log.info("URL 메타데이터에서 제목 추출 및 업데이트: {} -> {}", url, title);
            }
            if (metadata[1] != null && !metadata[1].isEmpty() && (content == null || content.isEmpty())) {
                // 원본 기사의 content도 업데이트 (없는 경우에만)
                article.setContent(metadata[1]);
                content = metadata[1];
                log.info("URL 메타데이터에서 설명 추출 및 업데이트: {}", url);
            }
        }

        // 제목이 여전히 없는 경우
        if (title == null || title.isEmpty()) {
            if (url != null && !url.isEmpty()) {
                // URL 경로에서 제목 추측
                title = extractTitleFromUrl(url);
                article.setTitle(title);
                log.info("URL 경로에서 제목 추출: {} -> {}", url, title);
            } else {
                log.warn("분석할 내용이 없는 기사: {}", article.getId());
                return;
            }
        }

        // 본문이 없는 경우 메타데이터 재사용 또는 제목 사용
        if (content == null || content.isEmpty()) {
            if (metadata != null && metadata[1] != null && !metadata[1].isEmpty()) {
                content = metadata[1];
            } else {
                content = title; // 본문이 없으면 제목 사용
            }
        }

        // 본문 길이 제한 (토큰 절약)
        if (content.length() > 3000) {
            content = content.substring(0, 3000) + "...";
        }

        String prompt = String.format(SUMMARY_PROMPT, title != null ? title : "", content);

        try {
            String response;
            String provider = aiConfig.getProvider();

            if ("claude-cli".equalsIgnoreCase(provider)) {
                // Claude CLI headless 모드 사용
                response = callClaudeCli(prompt);
            } else if ("claude".equalsIgnoreCase(provider)) {
                response = callClaudeApi(prompt);
            } else {
                response = callOpenAiApi(prompt);
            }

            if (response != null) {
                parseSummaryResponse(article.getId(), response);
            }
        } catch (Exception e) {
            log.error("AI 요약 실패: {} - {}", article.getId(), e.getMessage());
        }
    }

    /**
     * Claude CLI를 headless 모드로 실행하여 프롬프트 처리
     */
    private String callClaudeCli(String prompt) {
        try {
            String command = aiConfig.getClaudeCli().getCommand();
            int timeout = aiConfig.getClaudeCli().getTimeout();

            log.info("Claude CLI 실행 시작 (timeout: {}초)", timeout);

            // Claude CLI 명령어 구성 - stdin으로 프롬프트 전달
            ProcessBuilder processBuilder = new ProcessBuilder(
                command,
                "--print",  // 결과만 출력 (대화형 모드 비활성화)
                "--output-format", "text"  // 텍스트 형식으로 출력
            );

            // UTF-8 인코딩 환경 설정
            processBuilder.environment().put("LANG", "ko_KR.UTF-8");
            processBuilder.environment().put("LC_ALL", "ko_KR.UTF-8");
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            // 프롬프트를 stdin으로 전달
            try (OutputStreamWriter writer = new OutputStreamWriter(
                    process.getOutputStream(), StandardCharsets.UTF_8)) {
                writer.write(prompt);
                writer.flush();
            }

            // 응답 읽기
            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line).append("\n");
                }
            }

            // 프로세스 종료 대기
            boolean finished = process.waitFor(timeout, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.error("Claude CLI 타임아웃 ({}초 초과)", timeout);
                return null;
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.error("Claude CLI 오류 (exit code: {}): {}", exitCode, response);
                return null;
            }

            log.info("Claude CLI 응답 수신 완료");
            log.debug("Claude CLI 응답 내용: {}", response);
            return response.toString().trim();

        } catch (Exception e) {
            log.error("Claude CLI 실행 실패: {}", e.getMessage(), e);
            return null;
        }
    }

    private String callOpenAiApi(String prompt) {
        if (aiConfig.getOpenai().getApiKey() == null || aiConfig.getOpenai().getApiKey().isEmpty()) {
            log.warn("OpenAI API 키가 설정되지 않았습니다");
            return null;
        }

        String url = aiConfig.getOpenai().getBaseUrl() + "/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(aiConfig.getOpenai().getApiKey());

        Map<String, Object> requestBody = Map.of(
                "model", aiConfig.getOpenai().getModel(),
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.3,
                "max_tokens", 500
        );

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("choices").path(0).path("message").path("content").asText();
        } catch (Exception e) {
            log.error("OpenAI API 호출 실패: {}", e.getMessage());
            return null;
        }
    }

    private String callClaudeApi(String prompt) {
        if (aiConfig.getClaude().getApiKey() == null || aiConfig.getClaude().getApiKey().isEmpty()) {
            log.warn("Claude API 키가 설정되지 않았습니다");
            return null;
        }

        String url = aiConfig.getClaude().getBaseUrl() + "/messages";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", aiConfig.getClaude().getApiKey());
        headers.set("anthropic-version", "2023-06-01");

        Map<String, Object> requestBody = Map.of(
                "model", aiConfig.getClaude().getModel(),
                "max_tokens", 500,
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                )
        );

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("content").path(0).path("text").asText();
        } catch (Exception e) {
            log.error("Claude API 호출 실패: {}", e.getMessage());
            return null;
        }
    }

    private void parseSummaryResponse(Long articleId, String response) {
        try {
            // JSON 추출 (응답에 다른 텍스트가 포함될 수 있음)
            String jsonStr = extractJson(response);
            if (jsonStr == null) {
                log.warn("JSON 추출 실패: {}", response);
                return;
            }

            JsonNode json = objectMapper.readTree(jsonStr);

            String titleKo = json.path("titleKo").asText(null);
            String summary = json.path("summary").asText();
            double relevanceScore = json.path("relevanceScore").asDouble(0.5);
            String categoryStr = json.path("category").asText("OTHER");
            String importanceStr = json.path("importance").asText("MEDIUM");

            NewsArticle.ArticleCategory category;
            try {
                category = NewsArticle.ArticleCategory.valueOf(categoryStr);
            } catch (IllegalArgumentException e) {
                category = NewsArticle.ArticleCategory.OTHER;
            }

            NewsArticle.ArticleImportance importance;
            try {
                importance = NewsArticle.ArticleImportance.valueOf(importanceStr);
            } catch (IllegalArgumentException e) {
                importance = NewsArticle.ArticleImportance.MEDIUM;
            }

            newsArticleService.updateSummary(articleId, titleKo, summary, relevanceScore, category, importance);
            log.info("기사 요약 완료: {} (관련성: {}, 카테고리: {}, 제목: {})", articleId, relevanceScore, category, titleKo);

        } catch (Exception e) {
            log.error("요약 응답 파싱 실패: {} - {}", articleId, e.getMessage());
        }
    }

    private String extractJson(String response) {
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        return null;
    }

    /**
     * URL에서 og:title, og:description 메타데이터를 가져옴
     * @return [0]: og:title, [1]: og:description
     */
    private String[] fetchMetadataFromUrl(String urlStr) {
        String[] result = new String[2];
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (compatible; AIInsight/1.0)");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                log.warn("URL 메타데이터 가져오기 실패 (HTTP {}): {}", responseCode, urlStr);
                return result;
            }

            StringBuilder html = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                int lineCount = 0;
                while ((line = reader.readLine()) != null && lineCount < 100) {
                    html.append(line);
                    lineCount++;
                    // head 태그 끝나면 더 이상 읽지 않음
                    if (line.contains("</head>")) break;
                }
            }

            String htmlStr = html.toString();

            // og:title 추출 (property와 content 속성 순서가 바뀔 수 있음)
            // 패턴 1: property="og:title" content="..."
            Pattern ogTitlePattern1 = Pattern.compile("<meta\\s+property=[\"']og:title[\"']\\s+content=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
            // 패턴 2: content="..." property="og:title"
            Pattern ogTitlePattern2 = Pattern.compile("<meta\\s+content=[\"']([^\"']+)[\"']\\s+property=[\"']og:title[\"']", Pattern.CASE_INSENSITIVE);

            Matcher ogTitleMatcher1 = ogTitlePattern1.matcher(htmlStr);
            Matcher ogTitleMatcher2 = ogTitlePattern2.matcher(htmlStr);
            if (ogTitleMatcher1.find()) {
                result[0] = decodeHtmlEntities(ogTitleMatcher1.group(1));
            } else if (ogTitleMatcher2.find()) {
                result[0] = decodeHtmlEntities(ogTitleMatcher2.group(1));
            }

            // og:description 추출 (property와 content 속성 순서가 바뀔 수 있음)
            // 패턴 1: property="og:description" content="..."
            Pattern ogDescPattern1 = Pattern.compile("<meta\\s+property=[\"']og:description[\"']\\s+content=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
            // 패턴 2: content="..." property="og:description"
            Pattern ogDescPattern2 = Pattern.compile("<meta\\s+content=[\"']([^\"']+)[\"']\\s+property=[\"']og:description[\"']", Pattern.CASE_INSENSITIVE);

            Matcher ogDescMatcher1 = ogDescPattern1.matcher(htmlStr);
            Matcher ogDescMatcher2 = ogDescPattern2.matcher(htmlStr);
            if (ogDescMatcher1.find()) {
                result[1] = decodeHtmlEntities(ogDescMatcher1.group(1));
            } else if (ogDescMatcher2.find()) {
                result[1] = decodeHtmlEntities(ogDescMatcher2.group(1));
            }

            // name="description" 메타 태그도 시도 (og:description이 없는 경우)
            if (result[1] == null || result[1].isEmpty()) {
                Pattern metaDescPattern1 = Pattern.compile("<meta\\s+name=[\"']description[\"']\\s+content=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
                Pattern metaDescPattern2 = Pattern.compile("<meta\\s+content=[\"']([^\"']+)[\"']\\s+name=[\"']description[\"']", Pattern.CASE_INSENSITIVE);
                Matcher metaDescMatcher1 = metaDescPattern1.matcher(htmlStr);
                Matcher metaDescMatcher2 = metaDescPattern2.matcher(htmlStr);
                if (metaDescMatcher1.find()) {
                    result[1] = decodeHtmlEntities(metaDescMatcher1.group(1));
                } else if (metaDescMatcher2.find()) {
                    result[1] = decodeHtmlEntities(metaDescMatcher2.group(1));
                }
            }

            // og:title이 없으면 일반 title 태그 시도
            if (result[0] == null || result[0].isEmpty()) {
                Pattern titlePattern = Pattern.compile("<title>([^<]+)</title>", Pattern.CASE_INSENSITIVE);
                Matcher titleMatcher = titlePattern.matcher(htmlStr);
                if (titleMatcher.find()) {
                    result[0] = decodeHtmlEntities(titleMatcher.group(1));
                }
            }

            log.debug("URL 메타데이터 추출 완료: {} -> {}", urlStr, result[0]);

        } catch (Exception e) {
            log.warn("URL 메타데이터 가져오기 실패: {} - {}", urlStr, e.getMessage());
        }
        return result;
    }

    /**
     * HTML 엔티티 디코딩
     */
    private String decodeHtmlEntities(String text) {
        if (text == null) return null;
        return text
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&apos;", "'")
                .replace("&#x27;", "'")
                .replace("&hellip;", "...")
                .replace("&nbsp;", " ");
    }

    /**
     * URL이 이미지/미디어 파일인지 확인
     */
    private boolean isMediaUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        String lowerUrl = url.toLowerCase();
        // 이미지 확장자
        String[] mediaExtensions = {".png", ".jpg", ".jpeg", ".gif", ".webp", ".svg", ".ico",
                                    ".bmp", ".tiff", ".mp4", ".mp3", ".wav", ".avi", ".mov", ".pdf"};
        for (String ext : mediaExtensions) {
            if (lowerUrl.endsWith(ext) || lowerUrl.contains(ext + "?")) {
                return true;
            }
        }
        // 일반적인 이미지 호스팅 패턴
        String[] mediaPatterns = {"/wp-content/uploads/", "/images/", "/media/",
                                  "cdn.images.", "img.", "image.", "static/images"};
        for (String pattern : mediaPatterns) {
            if (lowerUrl.contains(pattern) && (lowerUrl.endsWith(".png") || lowerUrl.endsWith(".jpg")
                    || lowerUrl.endsWith(".jpeg") || lowerUrl.endsWith(".gif") || lowerUrl.endsWith(".webp"))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 제목이 일반적인(의미 없는) 제목인지 확인
     * 예: "기사 상세 보기", "Article View" 등
     */
    private boolean isGenericTitle(String title) {
        if (title == null || title.isEmpty()) {
            return true;
        }
        String lowerTitle = title.toLowerCase().trim();

        // 일반적인 제목 패턴들
        String[] genericPatterns = {
            "기사 상세",
            "기사 보기",
            "상세 보기",
            "article view",
            "article detail",
            "view article",
            "untitled",
            "no title",
            "제목 없음",
            "loading",
            "로딩"
        };

        for (String pattern : genericPatterns) {
            if (lowerTitle.contains(pattern)) {
                return true;
            }
        }

        // 제목이 사이트 이름만 있는 경우 (예: "AI타임스", "TechCrunch")
        // 제목이 너무 짧은 경우 (5자 미만)
        if (title.length() < 5) {
            return true;
        }

        return false;
    }

    /**
     * URL에서 기사 제목을 추출 (URL 슬러그를 사람이 읽을 수 있는 형태로 변환)
     */
    private String extractTitleFromUrl(String url) {
        try {
            // URL에서 마지막 경로 부분 추출
            String path = url;
            if (path.contains("?")) {
                path = path.substring(0, path.indexOf("?"));
            }
            if (path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }

            // 마지막 슬래시 이후 부분 추출
            int lastSlash = path.lastIndexOf("/");
            if (lastSlash >= 0) {
                path = path.substring(lastSlash + 1);
            }

            // 확장자 제거
            if (path.contains(".")) {
                path = path.substring(0, path.lastIndexOf("."));
            }

            // 하이픈, 언더스코어를 공백으로 변환하고 대문자화
            String title = path
                    .replace("-", " ")
                    .replace("_", " ")
                    .replaceAll("\\d{4,}", "") // 긴 숫자 제거 (날짜 등)
                    .trim();

            // 첫 글자 대문자로
            if (!title.isEmpty()) {
                title = title.substring(0, 1).toUpperCase() + title.substring(1);
            }

            return title.isEmpty() ? "Untitled Article" : title;
        } catch (Exception e) {
            log.warn("URL에서 제목 추출 실패: {}", url);
            return "Untitled Article";
        }
    }
}
