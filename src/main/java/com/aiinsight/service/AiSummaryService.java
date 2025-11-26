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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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

        기사 제목: %s
        기사 내용: %s

        다음 형식의 JSON으로 응답해주세요:
        {
            "titleKo": "기사 제목을 자연스러운 한국어로 번역",
            "summary": "3-5문장으로 핵심 내용을 한국어로 요약",
            "relevanceScore": 0.0-1.0 사이의 AI 관련성 점수,
            "category": "LLM|COMPUTER_VISION|NLP|ROBOTICS|ML_OPS|RESEARCH|INDUSTRY|STARTUP|REGULATION|TUTORIAL|PRODUCT|OTHER 중 하나",
            "importance": "HIGH|MEDIUM|LOW 중 하나"
        }

        카테고리 설명:
        - LLM: 대규모 언어 모델 (GPT, Claude, Llama 등)
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
        - HIGH: 업계 전반에 영향을 미치는 중요한 뉴스
        - MEDIUM: 특정 분야에 유의미한 뉴스
        - LOW: 일반적인 소식

        주의사항:
        - titleKo는 원문 제목을 자연스러운 한국어로 번역해주세요
        - summary도 한국어로 작성해주세요
        - JSON만 출력하세요.
        """;

    public void summarizeArticle(NewsArticle article) {
        if (article.getIsSummarized()) {
            log.debug("이미 요약된 기사: {}", article.getId());
            return;
        }

        String content = article.getContent();
        if (content == null || content.isEmpty()) {
            content = article.getTitle(); // 본문이 없으면 제목만 사용
        }

        // 본문 길이 제한 (토큰 절약)
        if (content.length() > 3000) {
            content = content.substring(0, 3000) + "...";
        }

        String prompt = String.format(SUMMARY_PROMPT, article.getTitle(), content);

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
}
