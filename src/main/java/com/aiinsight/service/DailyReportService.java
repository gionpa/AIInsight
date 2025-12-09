package com.aiinsight.service;

import com.aiinsight.domain.article.NewsArticle;
import com.aiinsight.domain.article.NewsArticleRepository;
import com.aiinsight.domain.embedding.ArticleEmbedding;
import com.aiinsight.domain.embedding.ArticleEmbeddingRepository;
import com.aiinsight.domain.report.DailyReport;
import com.aiinsight.domain.report.DailyReportRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 일일 리포트 생성 서비스
 * - 임베딩 기반 토픽 클러스터링
 * - Executive Summary 생성 (Claude CLI)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DailyReportService {

    private final DailyReportRepository reportRepository;
    private final NewsArticleRepository articleRepository;
    private final ArticleEmbeddingRepository embeddingRepository;
    private final EmbeddingService embeddingService;
    private final ObjectMapper objectMapper;

    /**
     * 특정 날짜의 일일 리포트 생성
     * @param targetDate 리포트 대상 날짜
     * @return 생성된 DailyReport
     */
    @Transactional
    public DailyReport generateDailyReport(LocalDate targetDate) {
        log.info("일일 리포트 생성 시작: {}", targetDate);

        // 1. 이미 생성된 리포트가 있는지 확인
        Optional<DailyReport> existingReport = reportRepository.findByReportDate(targetDate);
        if (existingReport.isPresent()) {
            log.info("이미 생성된 리포트 존재: {}", targetDate);
            return existingReport.get();
        }

        // 2. 해당 날짜의 HIGH 중요도 기사 조회
        LocalDateTime startOfDay = targetDate.atStartOfDay();
        LocalDateTime endOfDay = targetDate.plusDays(1).atStartOfDay();

        List<NewsArticle> highImportanceArticles = articleRepository.findByImportanceAndPublishedAtBetween(
                NewsArticle.ArticleImportance.HIGH,
                startOfDay,
                endOfDay
        );

        log.info("HIGH 중요도 기사 수: {}", highImportanceArticles.size());

        if (highImportanceArticles.isEmpty()) {
            log.warn("HIGH 중요도 기사가 없어 리포트 생성 불가: {}", targetDate);
            return null;
        }

        // 3. 임베딩이 있는 기사만 필터링
        List<NewsArticle> articlesWithEmbedding = highImportanceArticles.stream()
                .filter(article -> embeddingRepository.existsByArticle(article))
                .collect(Collectors.toList());

        log.info("임베딩이 있는 기사 수: {}", articlesWithEmbedding.size());

        if (articlesWithEmbedding.isEmpty()) {
            log.warn("임베딩이 있는 기사가 없어 리포트 생성 불가: {}", targetDate);
            return null;
        }

        // 4. 토픽 클러스터링 수행
        List<TopicCluster> topicClusters = performTopicClustering(articlesWithEmbedding);
        log.info("토픽 클러스터 수: {}", topicClusters.size());

        // 5. Executive Summary 생성
        String executiveSummary = generateExecutiveSummary(articlesWithEmbedding, topicClusters);

        // 6. 키 트렌드 추출
        String keyTrends = extractKeyTrends(articlesWithEmbedding);

        // 7. 토픽 요약 생성
        String topicSummaries = generateTopicSummaries(topicClusters);

        // 8. DailyReport 엔티티 생성 및 저장
        DailyReport report = DailyReport.builder()
                .reportDate(targetDate)
                .executiveSummary(executiveSummary)
                .keyTrends(keyTrends)
                .topicSummaries(topicSummaries)
                .topicClusters(serializeTopicClusters(topicClusters))
                .totalArticles(articlesWithEmbedding.size())
                .highImportanceArticles(articlesWithEmbedding.size())
                .status(DailyReport.ReportStatus.COMPLETED)
                .build();

        // 기사와 연결
        report.setArticles(articlesWithEmbedding);

        DailyReport savedReport = reportRepository.save(report);
        log.info("일일 리포트 생성 완료: {} (ID: {})", targetDate, savedReport.getId());

        return savedReport;
    }

    /**
     * 토픽 클러스터링 수행
     * - 간단한 유사도 기반 클러스터링 (K-means 대신 greedy approach)
     */
    private List<TopicCluster> performTopicClustering(List<NewsArticle> articles) {
        List<TopicCluster> clusters = new ArrayList<>();
        Set<Long> processedArticleIds = new HashSet<>();

        // 각 기사를 중심으로 유사한 기사를 찾아 클러스터 생성
        for (NewsArticle article : articles) {
            if (processedArticleIds.contains(article.getId())) {
                continue;
            }

            // 유사 기사 찾기 (유사도 > 0.7)
            List<Map<String, Object>> similarArticles = embeddingService.findSimilarArticles(
                    article.getId(),
                    10  // 최대 10개
            );

            List<NewsArticle> clusterArticles = new ArrayList<>();
            clusterArticles.add(article);
            processedArticleIds.add(article.getId());

            // 유사도가 높은 기사를 클러스터에 추가
            for (Map<String, Object> similar : similarArticles) {
                Long similarArticleId = (Long) similar.get("articleId");
                Double similarity = (Double) similar.get("similarity");

                if (similarity > 0.7 && !processedArticleIds.contains(similarArticleId)) {
                    NewsArticle similarArticle = articleRepository.findById(similarArticleId).orElse(null);
                    if (similarArticle != null) {
                        clusterArticles.add(similarArticle);
                        processedArticleIds.add(similarArticleId);
                    }
                }
            }

            // 클러스터 생성 (최소 1개 이상의 기사)
            if (!clusterArticles.isEmpty()) {
                TopicCluster cluster = new TopicCluster();
                cluster.setArticles(clusterArticles);
                cluster.setTopicName(extractTopicName(clusterArticles));
                clusters.add(cluster);
            }
        }

        // 클러스터 크기 기준 정렬 (큰 것부터)
        clusters.sort((c1, c2) -> Integer.compare(c2.getArticles().size(), c1.getArticles().size()));

        return clusters;
    }

    /**
     * 클러스터에서 토픽 이름 추출
     * - 가장 많이 등장하는 키워드 기반
     */
    private String extractTopicName(List<NewsArticle> articles) {
        // 제목에서 키워드 추출 (간단한 버전)
        Map<String, Integer> keywordFrequency = new HashMap<>();

        for (NewsArticle article : articles) {
            String title = article.getTitleKo() != null ? article.getTitleKo() : article.getTitle();
            if (title != null) {
                String[] words = title.split("\\s+");
                for (String word : words) {
                    if (word.length() > 2) {  // 2글자 이상만
                        keywordFrequency.put(word, keywordFrequency.getOrDefault(word, 0) + 1);
                    }
                }
            }
        }

        // 가장 빈도가 높은 키워드 찾기
        return keywordFrequency.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("기타");
    }

    /**
     * Executive Summary 생성 (간단한 버전)
     * TODO: Claude CLI를 사용한 AI 기반 요약으로 개선 필요
     */
    private String generateExecutiveSummary(List<NewsArticle> articles, List<TopicCluster> clusters) {
        StringBuilder summary = new StringBuilder();
        summary.append(String.format("오늘 총 %d개의 주요 AI 뉴스가 수집되었습니다. ", articles.size()));

        if (!clusters.isEmpty()) {
            summary.append(String.format("%d개의 주요 토픽으로 분류되었으며, ", clusters.size()));

            // 상위 3개 토픽 언급
            for (int i = 0; i < Math.min(clusters.size(), 3); i++) {
                TopicCluster cluster = clusters.get(i);
                summary.append(String.format("\"%s\" 관련 %d개 기사",
                    cluster.getTopicName(), cluster.getArticles().size()));
                if (i < Math.min(clusters.size(), 3) - 1) {
                    summary.append(", ");
                }
            }
            summary.append("가 가장 많이 보도되었습니다.");
        }

        return summary.toString();
    }

    /**
     * 키 트렌드 추출 (JSON 형식)
     */
    private String extractKeyTrends(List<NewsArticle> articles) {
        Map<String, Integer> keywordFrequency = new HashMap<>();

        for (NewsArticle article : articles) {
            String title = article.getTitleKo() != null ? article.getTitleKo() : article.getTitle();
            if (title != null) {
                String[] words = title.split("\\s+");
                for (String word : words) {
                    if (word.length() > 2) {
                        keywordFrequency.put(word, keywordFrequency.getOrDefault(word, 0) + 1);
                    }
                }
            }
        }

        // 상위 10개 키워드 추출
        List<Map<String, Object>> trends = keywordFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .map(entry -> {
                    Map<String, Object> trend = new HashMap<>();
                    trend.put("keyword", entry.getKey());
                    trend.put("frequency", entry.getValue());
                    return trend;
                })
                .collect(Collectors.toList());

        try {
            return objectMapper.writeValueAsString(trends);
        } catch (JsonProcessingException e) {
            log.error("키 트렌드 JSON 변환 실패", e);
            return "[]";
        }
    }

    /**
     * 토픽별 요약 생성
     */
    private String generateTopicSummaries(List<TopicCluster> clusters) {
        List<Map<String, Object>> summaries = new ArrayList<>();

        for (TopicCluster cluster : clusters) {
            Map<String, Object> summary = new HashMap<>();
            summary.put("topic", cluster.getTopicName());
            summary.put("articleCount", cluster.getArticles().size());

            // 대표 기사 제목
            List<String> titles = cluster.getArticles().stream()
                    .limit(3)
                    .map(article -> article.getTitleKo() != null ? article.getTitleKo() : article.getTitle())
                    .collect(Collectors.toList());
            summary.put("representativeTitles", titles);

            summaries.add(summary);
        }

        try {
            return objectMapper.writeValueAsString(summaries);
        } catch (JsonProcessingException e) {
            log.error("토픽 요약 JSON 변환 실패", e);
            return "[]";
        }
    }

    /**
     * 토픽 클러스터를 JSON으로 직렬화
     */
    private String serializeTopicClusters(List<TopicCluster> clusters) {
        List<Map<String, Object>> serialized = new ArrayList<>();

        for (TopicCluster cluster : clusters) {
            Map<String, Object> clusterMap = new HashMap<>();
            clusterMap.put("topic", cluster.getTopicName());
            clusterMap.put("articleIds", cluster.getArticles().stream()
                    .map(NewsArticle::getId)
                    .collect(Collectors.toList()));
            serialized.add(clusterMap);
        }

        try {
            return objectMapper.writeValueAsString(serialized);
        } catch (JsonProcessingException e) {
            log.error("토픽 클러스터 JSON 변환 실패", e);
            return "[]";
        }
    }

    /**
     * 토픽 클러스터 내부 클래스
     */
    private static class TopicCluster {
        private String topicName;
        private List<NewsArticle> articles;

        public String getTopicName() {
            return topicName;
        }

        public void setTopicName(String topicName) {
            this.topicName = topicName;
        }

        public List<NewsArticle> getArticles() {
            return articles;
        }

        public void setArticles(List<NewsArticle> articles) {
            this.articles = articles;
        }
    }

    /**
     * 특정 날짜의 리포트 조회
     */
    @Transactional(readOnly = true)
    public Optional<DailyReport> getReportByDate(LocalDate date) {
        return reportRepository.findByReportDate(date);
    }

    /**
     * 최근 리포트 조회
     */
    @Transactional(readOnly = true)
    public Optional<DailyReport> getLatestReport() {
        return reportRepository.findTopByOrderByReportDateDesc();
    }
}
