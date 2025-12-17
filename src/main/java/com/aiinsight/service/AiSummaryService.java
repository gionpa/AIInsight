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
    private final EmbeddingService embeddingService;
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
            "importance": "HIGH|MEDIUM|LOW 중 하나",
            "urgencyLevel": "BREAKING|TIMELY|EVERGREEN 중 하나",
            "impactScope": "GLOBAL|REGIONAL|SECTOR_SPECIFIC 중 하나",
            "businessImpact": 0.0-1.0 사이의 비즈니스 영향도,
            "actionabilityScore": 0.0-1.0 사이의 실용성 점수,
            "mentionedCompanies": ["OpenAI", "Google"] 형태의 배열 (최대 5개)
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

        긴급도 기준 (urgencyLevel):
        - BREAKING: 24시간 내 즉시 알려야 하는 속보 (신제품 출시, 주요 인수합병, 중대 발표)
        - TIMELY: 1주일 내 유의미한 시의성 있는 뉴스 (컨퍼런스, 업데이트, 이벤트)
        - EVERGREEN: 시간이 지나도 가치가 유지되는 콘텐츠 (튜토리얼, 연구 논문, 분석 리포트)

        영향 범위 (impactScope):
        - GLOBAL: 전 세계 AI 업계에 영향 (GPT-5 출시, 주요 규제)
        - REGIONAL: 특정 지역/국가에 영향 (한국 AI 정책, EU AI Act)
        - SECTOR_SPECIFIC: 특정 산업/분야에 한정 (의료 AI, 금융 AI)

        비즈니스 영향도 (businessImpact):
        - 0.9-1.0: 시장 판도를 바꿀 수 있는 영향 (GPT-5, 주요 M&A)
        - 0.7-0.9: 경쟁력에 큰 영향 (신규 경쟁자, 가격 정책 변화)
        - 0.5-0.7: 특정 분야에 유의미한 영향
        - 0.3-0.5: 참고할 만한 변화
        - 0.0-0.3: 제한적인 영향

        실용성 점수 (actionabilityScore):
        - 0.8-1.0: 즉시 적용 가능 (API 사용법, 코드 예제, 도구 출시)
        - 0.6-0.8: 도입 검토 가능 (신규 서비스, 베타 기능)
        - 0.4-0.6: 중장기 모니터링 필요 (트렌드, 연구 동향)
        - 0.0-0.4: 정보 참고용 (시장 분석, 의견/논평)

        언급된 회사 (mentionedCompanies):
        - 기사에서 직접 언급된 주요 기업명 추출 (최대 5개)
        - 예: ["OpenAI", "Google", "Anthropic", "Microsoft", "Meta"]
        - 일반 용어(AI, LLM)나 제품명만 있으면 빈 배열

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
     * - 타임아웃: 최대 3분 (Claude CLI timeout + 여유시간)
     * - 스레드 누수 방지를 위한 타임아웃 적용
     * @param articleId 분석할 기사 ID
     */
    @Async("aiAnalysisExecutor")
    public void summarizeArticleAsync(Long articleId) {
        long startTime = System.currentTimeMillis();
        try {
            NewsArticle article = newsArticleService.findEntityById(articleId);
            if (article != null) {
                // 타임아웃 체크를 위한 Thread interruption 처리
                if (Thread.interrupted()) {
                    log.warn("AI 분석 작업이 인터럽트됨 (기사 ID: {})", articleId);
                    Thread.currentThread().interrupt();
                    return;
                }

                summarizeArticle(article, false);

                long duration = System.currentTimeMillis() - startTime;
                log.info("AI 분석 완료 (기사 ID: {}, 소요시간: {}ms)", articleId, duration);
            }
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("비동기 AI 분석 실패 (기사 ID: {}, 소요시간: {}ms): {}",
                    articleId, duration, e.getMessage());
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

        // 상태: 분석 중
        newsArticleService.updateAnalysisStatus(article.getId(), NewsArticle.AnalysisStatus.PROCESSING);

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

        // 제목이 없거나 일반적인 제목인 경우, 또는 강제 재분석 모드인 경우 URL에서 정보 추출 시도
        boolean needMetadataFetch = forceReanalyze || (title == null || title.isEmpty() || isGenericTitle(title));
        String[] metadata = null;

        if (needMetadataFetch && url != null && !url.isEmpty()) {
            metadata = fetchMetadataFromUrl(url);
            if (metadata[0] != null && !metadata[0].isEmpty()) {
                // 원본 기사의 title도 업데이트 (DB 반영)
                article.setTitle(metadata[0]);
                title = metadata[0];
                log.info("URL 메타데이터에서 제목 추출 및 업데이트: {} -> {}", url, title);
            }
            if (metadata[1] != null && !metadata[1].isEmpty()) {
                // 강제 재분석 모드이거나 기존 content가 없는 경우 업데이트
                if (forceReanalyze || content == null || content.isEmpty()) {
                    article.setContent(metadata[1]);
                    content = metadata[1];
                    log.info("URL 메타데이터에서 본문 추출 및 업데이트 (강제={}, 기존 content 길이={})",
                            forceReanalyze, content == null ? 0 : content.length());
                }
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

        // 제목/본문 변경 사항 DB 반영
        newsArticleService.updateTitleAndContent(article.getId(), title, content);

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

            boolean parsed = false;
            if (response != null) {
                parsed = parseSummaryResponse(article.getId(), response);

                if (parsed) {
                    // 중요도 HIGH 기사에 대해서만 임베딩 생성
                    NewsArticle updatedArticle = newsArticleService.findEntityById(article.getId());
                    if (updatedArticle != null && updatedArticle.getImportance() == NewsArticle.ArticleImportance.HIGH) {
                        try {
                            embeddingService.generateAndSaveEmbedding(updatedArticle);
                            log.info("중요도 HIGH 기사 임베딩 생성 완료 (기사 ID: {})", article.getId());
                        } catch (Exception embeddingError) {
                            log.error("임베딩 생성 실패 (기사 ID: {}): {}", article.getId(), embeddingError.getMessage());
                        }
                    } else {
                        log.debug("중요도 HIGH가 아닌 기사는 임베딩 생성 건너뜀 (기사 ID: {}, 중요도: {})",
                                article.getId(), updatedArticle != null ? updatedArticle.getImportance() : "UNKNOWN");
                    }
                }
            }
            // 완료/실패 상태
            newsArticleService.updateAnalysisStatus(article.getId(),
                    parsed ? NewsArticle.AnalysisStatus.COMPLETED : NewsArticle.AnalysisStatus.FAILED);
        } catch (Exception e) {
            log.error("AI 요약 실패: {} - {}", article.getId(), e.getMessage());
            newsArticleService.updateAnalysisStatus(article.getId(), NewsArticle.AnalysisStatus.FAILED);
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

    private boolean parseSummaryResponse(Long articleId, String response) {
        try {
            // JSON 추출 (응답에 다른 텍스트가 포함될 수 있음)
            String jsonStr = extractJson(response);
            if (jsonStr == null) {
                log.warn("JSON 추출 실패: {}", response);
                return false;
            }

            JsonNode json = objectMapper.readTree(jsonStr);

            String titleKo = json.path("titleKo").asText(null);
            String summary = json.path("summary").asText();
            double relevanceScore = json.path("relevanceScore").asDouble(0.5);
            String categoryStr = json.path("category").asText("OTHER");
            String importanceStr = json.path("importance").asText("MEDIUM");

            // 새로운 지표들 파싱
            String urgencyLevelStr = json.path("urgencyLevel").asText("TIMELY");
            String impactScopeStr = json.path("impactScope").asText("SECTOR_SPECIFIC");
            Double businessImpact = json.path("businessImpact").asDouble(0.5);
            Double actionabilityScore = json.path("actionabilityScore").asDouble(0.5);

            // mentionedCompanies는 JSON 배열이므로 문자열로 저장
            String mentionedCompanies = null;
            JsonNode companiesNode = json.path("mentionedCompanies");
            if (companiesNode.isArray() && companiesNode.size() > 0) {
                mentionedCompanies = companiesNode.toString();
            }

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

            NewsArticle.UrgencyLevel urgencyLevel;
            try {
                urgencyLevel = NewsArticle.UrgencyLevel.valueOf(urgencyLevelStr);
            } catch (IllegalArgumentException e) {
                urgencyLevel = NewsArticle.UrgencyLevel.TIMELY;
            }

            NewsArticle.ImpactScope impactScope;
            try {
                impactScope = NewsArticle.ImpactScope.valueOf(impactScopeStr);
            } catch (IllegalArgumentException e) {
                impactScope = NewsArticle.ImpactScope.SECTOR_SPECIFIC;
            }

            newsArticleService.updateSummary(
                    articleId, titleKo, summary, relevanceScore, category, importance,
                    urgencyLevel, impactScope, businessImpact, actionabilityScore, mentionedCompanies
            );
            log.info("기사 요약 완료: {} (관련성: {}, 카테고리: {}, 긴급도: {}, 비즈니스 영향: {}, 제목: {})",
                    articleId, relevanceScore, category, urgencyLevel, businessImpact, titleKo);
            return true;
        } catch (Exception e) {
            log.error("요약 응답 파싱 실패: {} - {}", articleId, e.getMessage());
            return false;
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
        String[] result = new String[2]; // [0]: title, [1]: content
        try {
            log.info("URL 메타데이터 및 본문 크롤링 시작: {}", urlStr);

            // Jsoup을 사용하여 전체 HTML 문서 파싱
            org.jsoup.nodes.Document doc = org.jsoup.Jsoup.connect(urlStr)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .timeout(10000)
                    .followRedirects(true)
                    .get();

            // 1. 제목 추출 (우선순위: og:title > twitter:title > title tag)
            String title = doc.select("meta[property=og:title]").attr("content");
            if (title.isEmpty()) {
                title = doc.select("meta[name=twitter:title]").attr("content");
            }
            if (title.isEmpty()) {
                title = doc.title();
            }
            result[0] = decodeHtmlEntities(title.trim());
            log.info("제목 추출 성공: {}", result[0]);

            // 2. 본문 추출 시도 (여러 선택자로 시도)
            String content = "";

            // 방법 1: article 태그 내용
            org.jsoup.nodes.Element articleElem = doc.selectFirst("article");
            if (articleElem != null) {
                content = articleElem.text();
                log.info("article 태그에서 본문 추출 ({} chars)", content.length());
            }

            // 방법 2: main 태그 내용
            if (content.isEmpty()) {
                org.jsoup.nodes.Element mainElem = doc.selectFirst("main");
                if (mainElem != null) {
                    content = mainElem.text();
                    log.info("main 태그에서 본문 추출 ({} chars)", content.length());
                }
            }

            // 방법 3: 특정 클래스명으로 시도 (일반적인 기사 본문 클래스)
            if (content.isEmpty()) {
                org.jsoup.select.Elements contentElems = doc.select(
                    ".article-content, .post-content, .entry-content, " +
                    ".content, .article-body, .post-body, .story-body, " +
                    "[class*=article], [class*=content], [class*=body], " +
                    "[id*=article], [id*=content], [id*=body]"
                );
                if (!contentElems.isEmpty()) {
                    // 가장 긴 텍스트를 가진 요소 선택
                    content = contentElems.stream()
                            .map(org.jsoup.nodes.Element::text)
                            .max((a, b) -> Integer.compare(a.length(), b.length()))
                            .orElse("");
                    log.info("클래스/ID 선택자에서 본문 추출 ({} chars)", content.length());
                }
            }

            // 방법 4: p 태그들을 합쳐서 본문 추출 (최후의 수단)
            if (content.isEmpty() || content.length() < 200) {
                org.jsoup.select.Elements paragraphs = doc.select("p");
                if (!paragraphs.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (org.jsoup.nodes.Element p : paragraphs) {
                        String pText = p.text().trim();
                        if (pText.length() > 50) { // 최소 50자 이상의 문단만 포함
                            sb.append(pText).append(" ");
                        }
                    }
                    String pContent = sb.toString().trim();
                    if (pContent.length() > content.length()) {
                        content = pContent;
                        log.info("p 태그 집합에서 본문 추출 ({} chars)", content.length());
                    }
                }
            }

            // 3. OG description을 fallback으로 사용 (본문이 너무 짧은 경우)
            if (content.length() < 100) {
                String ogDesc = doc.select("meta[property=og:description]").attr("content");
                if (!ogDesc.isEmpty() && ogDesc.length() > content.length()) {
                    content = ogDesc;
                    log.info("og:description을 본문으로 사용 ({} chars)", content.length());
                }
            }

            // 4. 본문 정제 (불필요한 공백 제거, 길이 제한)
            content = content.replaceAll("\\s+", " ").trim();
            if (content.length() > 5000) {
                content = content.substring(0, 5000) + "...";
                log.info("본문 길이 제한 적용: 5000 chars");
            }

            result[1] = content;
            log.info("최종 본문 추출 완료: {} chars", content.length());

            return result;

        } catch (org.jsoup.HttpStatusException e) {
            log.warn("HTTP 오류로 URL 크롤링 실패 ({}): {}", e.getStatusCode(), urlStr);
            return result;
        } catch (java.net.SocketTimeoutException e) {
            log.warn("타임아웃으로 URL 크롤링 실패: {}", urlStr);
            return result;
        } catch (Exception e) {
            log.error("URL 크롤링 실패: {} - {}", urlStr, e.getMessage());
            return result;
        }
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
