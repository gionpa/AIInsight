package com.aiinsight.service;

import com.aiinsight.domain.article.NewsArticle;
import com.aiinsight.domain.embedding.ArticleEmbedding;
import com.aiinsight.domain.embedding.ArticleEmbeddingRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI Embedding API를 사용하여 기사의 임베딩 벡터를 생성하는 서비스
 * - Model: text-embedding-3-small (1536 dimensions)
 * - Cost: $0.02 per 1M tokens
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingService {

    private final ArticleEmbeddingRepository embeddingRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ai.embedding.provider:local-bge}")
    private String embeddingProvider; // local-bge | openai

    @Value("${ai.embedding.model:BAAI/bge-m3}")
    private String embeddingModel;

    @Value("${ai.embedding.endpoint:http://localhost:8081/embeddings}")
    private String embeddingEndpoint;

    @Value("${ai.embedding.dimension:1024}")
    private int embeddingDimension;

    @Value("${ai.openai.api-key:}")
    private String openAiApiKey;

    private static final String OPENAI_EMBEDDING_URL = "https://api.openai.com/v1/embeddings";

    /**
     * 기사의 임베딩을 생성하고 저장
     * @param article 임베딩을 생성할 기사
     * @return 생성된 ArticleEmbedding
     */
    @Transactional
    public ArticleEmbedding generateAndSaveEmbedding(NewsArticle article) {
        log.info("임베딩 생성 시작: 기사 ID {}", article.getId());

        try {
            // 이미 임베딩이 존재하는지 확인
            if (embeddingRepository.existsByArticle(article)) {
                log.info("이미 임베딩이 존재함: 기사 ID {}", article.getId());
                return embeddingRepository.findByArticle(article).orElse(null);
            }

            // 임베딩 텍스트 준비 (제목 + 요약 + 원본 요약)
            String embeddingText = prepareEmbeddingText(article);
            int tokenCount = estimateTokenCount(embeddingText);

            // 임베딩 생성 (provider에 따라 분기)
            List<Double> embeddingVector = switch (embeddingProvider.toLowerCase()) {
                case "openai" -> callOpenAiEmbeddingApi(embeddingText);
                case "local-bge", "local" -> callLocalEmbeddingApi(embeddingText);
                default -> throw new IllegalStateException("알 수 없는 임베딩 공급자: " + embeddingProvider);
            };

            // ArticleEmbedding 엔티티 생성
            ArticleEmbedding embedding = ArticleEmbedding.builder()
                    .article(article)
                    .embeddingVector(convertVectorToString(embeddingVector))
                    .modelName(embeddingModel)
                    .tokenCount(tokenCount)
                    .qualityScore(calculateQualityScore(article))
                    .createdAt(LocalDateTime.now())
                    .build();

            // 저장
            ArticleEmbedding saved = embeddingRepository.save(embedding);
            log.info("임베딩 저장 완료: 기사 ID {}, 토큰 수 {}", article.getId(), tokenCount);

            return saved;

        } catch (Exception e) {
            log.error("임베딩 생성 실패: 기사 ID {}, 오류: {}", article.getId(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * 임베딩 생성을 위한 텍스트 준비
     * - 우선순위: 한글 제목 > 영문 제목 > AI 요약 > 원본 요약
     */
    private String prepareEmbeddingText(NewsArticle article) {
        StringBuilder text = new StringBuilder();

        // 제목 (한글 우선, 없으면 영문)
        if (article.getTitleKo() != null && !article.getTitleKo().isBlank()) {
            text.append(article.getTitleKo()).append("\n\n");
        } else if (article.getTitle() != null && !article.getTitle().isBlank()) {
            text.append(article.getTitle()).append("\n\n");
        }

        // AI 요약 (있는 경우)
        if (article.getSummary() != null && !article.getSummary().isBlank()) {
            text.append(article.getSummary());
        }

        String result = text.toString().trim();

        // 텍스트가 너무 길면 잘라내기 (최대 8000자, 약 2000 토큰)
        if (result.length() > 8000) {
            result = result.substring(0, 8000);
        }

        return result;
    }

    /**
     * 토큰 수 추정 (대략 4자 = 1토큰)
     */
    private int estimateTokenCount(String text) {
        return text.length() / 4;
    }

    /**
     * 품질 점수 계산 (0.0 ~ 1.0)
     * - AI 분석 여부, 한글 제목 여부, 중요도 등을 고려
     */
    private Double calculateQualityScore(NewsArticle article) {
        double score = 0.5; // 기본 점수

        // AI 분석 완료 (+0.3)
        if (article.getSummary() != null && !article.getSummary().isBlank()) {
            score += 0.3;
        }

        // 한글 제목 존재 (+0.1)
        if (article.getTitleKo() != null && !article.getTitleKo().isBlank()) {
            score += 0.1;
        }

        // 중요도 HIGH (+0.1)
        if (article.getImportance() == NewsArticle.ArticleImportance.HIGH) {
            score += 0.1;
        }

        return Math.min(score, 1.0);
    }

    /**
     * OpenAI Embedding API 호출
     */
    private List<Double> callOpenAiEmbeddingApi(String text) throws Exception {
        if (openAiApiKey == null || openAiApiKey.isBlank()) {
            throw new IllegalStateException("OpenAI API 키가 설정되지 않았습니다.");
        }

        // HTTP 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + openAiApiKey);
        headers.set("Content-Type", "application/json");

        // 요청 바디 생성
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", embeddingModel);
        requestBody.put("input", text);
        requestBody.put("encoding_format", "float");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // API 호출
        ResponseEntity<String> response = restTemplate.exchange(
                OPENAI_EMBEDDING_URL,
                HttpMethod.POST,
                entity,
                String.class
        );

        // 응답 파싱
        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode embeddingNode = root.path("data").get(0).path("embedding");

        // List<Double>로 변환
        List<Double> embedding = objectMapper.convertValue(
                embeddingNode,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Double.class)
        );

        if (embedding.size() != embeddingDimension) {
            throw new IllegalStateException(
                    String.format("예상하지 못한 임베딩 차원: %d (예상: %d)",
                    embedding.size(), embeddingDimension)
            );
        }

        return embedding;
    }

    /**
     * 로컬 Hugging Face text-embeddings-inference 서버 호출 (BAAI/bge-m3)
     */
    private List<Double> callLocalEmbeddingApi(String text) throws Exception {
        // HTTP 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        // 요청 바디 생성 (text-embeddings-inference는 OpenAI 호환 형식을 사용)
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", embeddingModel);
        requestBody.put("input", text);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                embeddingEndpoint,
                HttpMethod.POST,
                entity,
                String.class
        );

        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode embeddingNode = root.path("data").get(0).path("embedding");

        List<Double> embedding = objectMapper.convertValue(
                embeddingNode,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Double.class)
        );

        if (embedding.size() != embeddingDimension) {
            throw new IllegalStateException(
                    String.format("예상하지 못한 임베딩 차원: %d (예상: %d)",
                            embedding.size(), embeddingDimension)
            );
        }

        return embedding;
    }

    /**
     * List<Double>를 PostgreSQL vector 타입 문자열로 변환
     * 예: [0.1, 0.2, 0.3] -> "[0.1,0.2,0.3]"
     */
    private String convertVectorToString(List<Double> vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(vector.get(i));
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * 임베딩이 없는 기사들에 대해 배치로 임베딩 생성
     * @param limit 한 번에 처리할 최대 기사 수
     * @return 생성된 임베딩 수
     */
    @Transactional
    public int generateEmbeddingsForArticlesWithoutEmbedding(int limit) {
        log.info("임베딩 배치 생성 시작: 최대 {}개", limit);

        List<NewsArticle> articlesWithoutEmbedding =
                embeddingRepository.findArticlesWithoutEmbedding(org.springframework.data.domain.PageRequest.of(0, limit));

        log.info("임베딩이 없는 기사: {}개", articlesWithoutEmbedding.size());

        int successCount = 0;
        for (NewsArticle article : articlesWithoutEmbedding) {
            try {
                ArticleEmbedding embedding = generateAndSaveEmbedding(article);
                if (embedding != null) {
                    successCount++;
                }

                // API Rate Limit 방지를 위한 짧은 대기 (100ms)
                Thread.sleep(100);

            } catch (Exception e) {
                log.error("배치 임베딩 생성 실패: 기사 ID {}", article.getId(), e);
            }
        }

        log.info("임베딩 배치 생성 완료: {}/{} 성공", successCount, articlesWithoutEmbedding.size());
        return successCount;
    }

    /**
     * 특정 기사와 유사한 기사 찾기 (코사인 유사도 기반)
     * @param articleId 기준 기사 ID
     * @param limit 반환할 최대 개수
     * @return 유사한 기사 ID와 유사도 점수 맵
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> findSimilarArticles(Long articleId, int limit) {
        ArticleEmbedding embedding = embeddingRepository.findByArticleId(articleId)
                .orElseThrow(() -> new IllegalArgumentException("임베딩이 없는 기사: " + articleId));

        List<Object[]> results = embeddingRepository.findSimilarArticles(
                embedding.getEmbeddingVector(),
                articleId,
                limit
        );

        return results.stream()
                .map(row -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("articleId", ((ArticleEmbedding) row[0]).getArticle().getId());
                    map.put("similarity", row[1]);
                    return map;
                })
                .toList();
    }
}
