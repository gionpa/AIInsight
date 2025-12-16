package com.aiinsight.service;

import com.aiinsight.config.AiConfig;
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 임베딩 RAG 기반 일일 리포트 생성 서비스
 * - 계층적 클러스터링을 통한 토픽 분석
 * - Claude AI를 활용한 Executive Summary 생성
 * - 최근 7일간 데이터를 활용한 트렌드 분석
 * - A4 한 페이지 분량의 고품질 리포트
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DailyReportService {

    private final DailyReportRepository reportRepository;
    private final NewsArticleRepository articleRepository;
    private final ArticleEmbeddingRepository embeddingRepository;
    private final EmbeddingService embeddingService;
    private final AiConfig aiConfig;
    private final ObjectMapper objectMapper;

    /**
     * RAG 기반 고도화된 월간 리포트 생성
     * - 최근 30일간의 HIGH 중요도 기사 분석
     * - 임베딩 기반 계층적 클러스터링
     * - Claude AI를 활용한 심층 분석 및 요약
     * - 시계열 트렌드 분석 및 주간 변화 추이
     *
     * @param targetDate 리포트 대상 날짜
     * @return 생성된 DailyReport
     */
    @Transactional
    public DailyReport generateDailyReport(LocalDate targetDate) {
        long startTime = System.currentTimeMillis();
        log.info("=== RAG 기반 월간 리포트 생성 시작: {} ===", targetDate);

        try {
            // 1. 기존 리포트 삭제 (재생성 허용)
            reportRepository.findByReportDate(targetDate).ifPresent(existing -> {
                log.info("기존 리포트 삭제 후 재생성: {}", targetDate);
                reportRepository.delete(existing);
            });

            // 2. 최근 30일간의 HIGH 중요도 기사 조회 (임베딩 필수)
            LocalDateTime endOfDay = targetDate.plusDays(1).atStartOfDay();
            LocalDateTime startOfPeriod = targetDate.minusDays(29).atStartOfDay(); // 30일 전부터

            List<NewsArticle> highImportanceArticles = articleRepository.findByImportanceAndCrawledAtBetween(
                    NewsArticle.ArticleImportance.HIGH,
                    startOfPeriod,
                    endOfDay
            );

            log.info("최근 30일간 HIGH 중요도 기사 수: {}", highImportanceArticles.size());

            // 3. 임베딩이 있는 기사만 필터링
            List<NewsArticle> articlesWithEmbedding = highImportanceArticles.stream()
                    .filter(article -> embeddingRepository.existsByArticle(article))
                    .collect(Collectors.toList());

            log.info("임베딩이 있는 HIGH 기사 수: {}", articlesWithEmbedding.size());

            if (articlesWithEmbedding.size() < 3) {
                log.warn("리포트 생성에 충분한 기사가 없음: {} (최소 3개 필요)", articlesWithEmbedding.size());
                return createErrorReport(targetDate, "충분한 데이터 없음 (임베딩 있는 기사 < 3개)");
            }

            // 4. 계층적 클러스터링 수행 (유사도 0.65 기준)
            List<TopicCluster> topicClusters = performHierarchicalClustering(articlesWithEmbedding, 0.65);
            log.info("계층적 클러스터링 결과: {}개 토픽", topicClusters.size());

            // 5. 각 클러스터에 대해 의미있는 토픽명 생성
            for (TopicCluster cluster : topicClusters) {
                cluster.setTopicName(extractSemanticTopicName(cluster.getArticles()));
            }

            // 6. 트렌드 분석 (7일 전 vs 오늘)
            TrendAnalysis trendAnalysis = analyzeTrends(targetDate, articlesWithEmbedding);
            log.info("트렌드 분석 완료: 신규 {}개, 증가 {}개, 감소 {}개",
                    trendAnalysis.emergingTopics.size(),
                    trendAnalysis.hotTopics.size(),
                    trendAnalysis.decliningTopics.size());

            // 7. Claude AI를 활용한 Executive Summary 생성
            String executiveSummary = generateAIExecutiveSummary(
                    targetDate,
                    articlesWithEmbedding,
                    topicClusters,
                    trendAnalysis
            );

            // 8. 카테고리 분포 계산
            String categoryDistribution = calculateCategoryDistribution(articlesWithEmbedding);

            // 9. 토픽별 AI 요약 생성
            String topicSummaries = generateAITopicSummaries(topicClusters);

            // 10. 키 트렌드 추출 (TF-IDF 기반)
            String keyTrends = extractTFIDFKeyTrends(articlesWithEmbedding);

            // 11. 품질 점수 계산
            double qualityScore = calculateReportQualityScore(articlesWithEmbedding, topicClusters);

            // 12. DailyReport 엔티티 생성 및 저장
            long duration = System.currentTimeMillis() - startTime;
            DailyReport report = DailyReport.builder()
                    .reportDate(targetDate)
                    .executiveSummary(executiveSummary)
                    .keyTrends(keyTrends)
                    .topicSummaries(topicSummaries)
                    .topicClusters(serializeTopicClusters(topicClusters))
                    .categoryDistribution(categoryDistribution)
                    .totalArticles(articlesWithEmbedding.size())
                    .highImportanceArticles(articlesWithEmbedding.size())
                    .avgRelevanceScore(calculateAvgRelevanceScore(articlesWithEmbedding))
                    .qualityScore(qualityScore)
                    .generationModel("claude-cli + bge-m3")
                    .generationDurationMs(duration)
                    .status(DailyReport.ReportStatus.COMPLETED)
                    .build();

            report.setArticles(articlesWithEmbedding);

            DailyReport savedReport = reportRepository.save(report);
            log.info("=== RAG 기반 리포트 생성 완료: {} (ID: {}, 소요시간: {}ms) ===",
                    targetDate, savedReport.getId(), duration);

            return savedReport;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("리포트 생성 실패: {} (소요시간: {}ms)", targetDate, duration, e);
            return createErrorReport(targetDate, "생성 중 오류 발생: " + e.getMessage());
        }
    }

    /**
     * 오류 리포트 생성
     */
    private DailyReport createErrorReport(LocalDate targetDate, String errorMessage) {
        DailyReport errorReport = DailyReport.builder()
                .reportDate(targetDate)
                .executiveSummary("리포트 생성 실패: " + errorMessage)
                .status(DailyReport.ReportStatus.FAILED)
                .errorMessage(errorMessage)
                .totalArticles(0)
                .highImportanceArticles(0)
                .build();
        return reportRepository.save(errorReport);
    }

    /**
     * 계층적 클러스터링 (Hierarchical Agglomerative Clustering)
     * - 모든 기사 쌍의 유사도 계산
     * - similarityThreshold 이상인 기사들을 클러스터로 그룹화
     *
     * @param articles 클러스터링할 기사 목록
     * @param similarityThreshold 클러스터링 유사도 임계값 (0.0 ~ 1.0)
     * @return 토픽 클러스터 목록
     */
    private List<TopicCluster> performHierarchicalClustering(
            List<NewsArticle> articles,
            double similarityThreshold
    ) {
        log.info("계층적 클러스터링 시작: 기사 {}개, 임계값 {}", articles.size(), similarityThreshold);

        List<TopicCluster> clusters = new ArrayList<>();
        Set<Long> processedArticleIds = new HashSet<>();

        // 각 기사에 대해 유사한 기사들을 재귀적으로 추가
        for (NewsArticle article : articles) {
            if (processedArticleIds.contains(article.getId())) {
                continue;
            }

            // 새 클러스터 생성
            List<NewsArticle> clusterArticles = new ArrayList<>();
            Queue<NewsArticle> queue = new LinkedList<>();

            queue.offer(article);
            processedArticleIds.add(article.getId());
            clusterArticles.add(article);

            // BFS로 유사한 기사들 추가
            while (!queue.isEmpty()) {
                NewsArticle current = queue.poll();

                // 현재 기사와 유사한 기사 찾기
                List<Map<String, Object>> similarArticles = embeddingService.findSimilarArticles(
                        current.getId(),
                        20  // 최대 20개 검색
                );

                for (Map<String, Object> similar : similarArticles) {
                    Long similarId = (Long) similar.get("articleId");
                    Double similarity = (Double) similar.get("similarity");

                    // 유사도가 임계값 이상이고 아직 처리되지 않은 기사
                    if (similarity >= similarityThreshold && !processedArticleIds.contains(similarId)) {
                        NewsArticle similarArticle = articleRepository.findById(similarId).orElse(null);
                        if (similarArticle != null && articles.contains(similarArticle)) {
                            processedArticleIds.add(similarId);
                            clusterArticles.add(similarArticle);
                            queue.offer(similarArticle);
                        }
                    }
                }
            }

            // 클러스터 생성
            if (!clusterArticles.isEmpty()) {
                TopicCluster cluster = new TopicCluster();
                cluster.setArticles(clusterArticles);
                clusters.add(cluster);
            }
        }

        // 클러스터 크기 기준 정렬 (큰 것부터)
        clusters.sort((c1, c2) -> Integer.compare(c2.getArticles().size(), c1.getArticles().size()));

        log.info("계층적 클러스터링 완료: {}개 클러스터 생성", clusters.size());
        return clusters;
    }

    /**
     * 의미적 토픽명 생성 (Centroid 기반)
     * - 클러스터의 대표 기사 찾기 (centroid에 가장 가까운 기사)
     *
     * @param clusterArticles 클러스터 내 기사 목록
     * @return 토픽 이름
     */
    private String extractSemanticTopicName(List<NewsArticle> clusterArticles) {
        if (clusterArticles.isEmpty()) {
            return "기타";
        }

        if (clusterArticles.size() == 1) {
            NewsArticle article = clusterArticles.get(0);
            String title = article.getTitleKo() != null ? article.getTitleKo() : article.getTitle();
            return title.length() > 40 ? title.substring(0, 40) + "..." : title;
        }

        // 클러스터 내에서 다른 모든 기사와의 평균 유사도가 가장 높은 기사 찾기
        NewsArticle representative = null;
        double maxAvgSimilarity = -1.0;

        for (NewsArticle candidate : clusterArticles) {
            List<Map<String, Object>> similarities = embeddingService.findSimilarArticles(
                    candidate.getId(),
                    clusterArticles.size()
            );

            // 클러스터 내 기사들과의 평균 유사도 계산
            double avgSimilarity = similarities.stream()
                    .filter(sim -> clusterArticles.stream()
                            .anyMatch(a -> a.getId().equals(sim.get("articleId"))))
                    .mapToDouble(sim -> (Double) sim.get("similarity"))
                    .average()
                    .orElse(0.0);

            if (avgSimilarity > maxAvgSimilarity) {
                maxAvgSimilarity = avgSimilarity;
                representative = candidate;
            }
        }

        if (representative != null) {
            String title = representative.getTitleKo() != null
                    ? representative.getTitleKo()
                    : representative.getTitle();
            return title.length() > 40 ? title.substring(0, 40) + "..." : title;
        }

        return "기타";
    }

    /**
     * 트렌드 분석 (30일 전 vs 최근 30일)
     * - 신규 등장 토픽
     * - 증가 중인 토픽
     * - 감소 중인 토픽
     */
    private TrendAnalysis analyzeTrends(LocalDate targetDate, List<NewsArticle> recentArticles) {
        TrendAnalysis analysis = new TrendAnalysis();

        // 30일 이전 기사 조회 (비교 기준: 30-60일 전)
        LocalDateTime thirtyDaysAgo = targetDate.minusDays(30).atStartOfDay();
        LocalDateTime sixtyDaysAgo = targetDate.minusDays(60).atStartOfDay();

        List<NewsArticle> oldArticles = articleRepository.findByImportanceAndCrawledAtBetween(
                NewsArticle.ArticleImportance.HIGH,
                sixtyDaysAgo,
                thirtyDaysAgo
        ).stream()
                .filter(article -> embeddingRepository.existsByArticle(article))
                .collect(Collectors.toList());

        log.info("트렌드 분석: 최근 30일 {}개, 과거(30-60일전) {}개",
                recentArticles.size(), oldArticles.size());

        // 카테고리별 기사 수 비교
        Map<NewsArticle.ArticleCategory, Integer> recentCategoryCounts = recentArticles.stream()
                .filter(a -> a.getCategory() != null)
                .collect(Collectors.groupingBy(
                        NewsArticle::getCategory,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));

        Map<NewsArticle.ArticleCategory, Integer> oldCategoryCounts = oldArticles.stream()
                .filter(a -> a.getCategory() != null)
                .collect(Collectors.groupingBy(
                        NewsArticle::getCategory,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));

        // 트렌드 분류
        for (NewsArticle.ArticleCategory category : recentCategoryCounts.keySet()) {
            int recentCount = recentCategoryCounts.get(category);
            int oldCount = oldCategoryCounts.getOrDefault(category, 0);

            if (oldCount == 0) {
                // 신규 등장
                analysis.emergingTopics.add(category.name());
            } else {
                double growthRate = ((double) (recentCount - oldCount) / oldCount) * 100;
                if (growthRate > 50) {
                    // 50% 이상 증가
                    analysis.hotTopics.add(category.name());
                } else if (growthRate < -30) {
                    // 30% 이상 감소
                    analysis.decliningTopics.add(category.name());
                } else {
                    // 안정적
                    analysis.stableTopics.add(category.name());
                }
            }
        }

        return analysis;
    }

    /**
     * Claude AI를 활용한 Executive Summary 생성
     * - 전체 리포트의 핵심 요약 (A4 절반 분량)
     */
    private String generateAIExecutiveSummary(
            LocalDate targetDate,
            List<NewsArticle> articles,
            List<TopicCluster> clusters,
            TrendAnalysis trendAnalysis
    ) {
        log.info("Claude AI Executive Summary 생성 시작");

        try {
            // 상위 5개 클러스터의 대표 기사만 사용 (토큰 절약)
            List<String> topArticleSummaries = clusters.stream()
                    .limit(5)
                    .map(cluster -> {
                        NewsArticle representative = cluster.getArticles().get(0);
                        return String.format("【%s】%s: %s",
                                cluster.getTopicName(),
                                representative.getTitleKo() != null
                                        ? representative.getTitleKo()
                                        : representative.getTitle(),
                                representative.getSummary() != null
                                        ? representative.getSummary()
                                        : "요약 없음"
                        );
                    })
                    .collect(Collectors.toList());

            String prompt = String.format("""
                    당신은 AI 업계 전문 애널리스트입니다. 최근 30일간의 주요 AI 뉴스를 분석하여 경영진용 월간 리포트를 작성해주세요.

                    **분석 기간**: %s 기준 최근 30일
                    **분석 대상**: 중요도 HIGH 기사 %d개
                    **식별된 주요 토픽**: %d개

                    **월간 트렌드 분석** (지난달 대비):
                    - 신규 등장 분야: %s
                    - 급성장 분야 (50%%↑): %s
                    - 감소 분야 (30%%↓): %s

                    **주요 토픽별 대표 기사** (Top 5):
                    %s

                    다음 형식으로 **A4 한 페이지 분량(약 1500-2000자)**의 월간 리포트를 한국어로 작성해주세요:

                    ## 1. Executive Summary (핵심 요약)
                    이번 달 AI 업계의 가장 중요한 변화와 핵심 메시지를 3-4문장으로 요약

                    ## 2. 주요 토픽 심층 분석
                    ### 토픽 1: [토픽명] (기사 X건)
                    - 핵심 내용: 무엇이 일어났는가? (2-3문장)
                    - 주요 이슈: 구체적인 사례와 수치
                    - 시사점: 업계에 미칠 영향과 의미

                    ### 토픽 2: [토픽명] (기사 X건)
                    - 핵심 내용
                    - 주요 이슈
                    - 시사점

                    ### 토픽 3-5: [간략한 요약]

                    ## 3. 월간 트렌드 인사이트
                    - **신규 등장**: 새롭게 주목받는 기술/서비스와 그 배경
                    - **급성장**: 빠르게 성장하는 분야와 성장 요인
                    - **주목할 변화**: 업계 판도에 영향을 줄 중요한 움직임

                    ## 4. 향후 전망 및 Action Items
                    - 단기 전망 (1개월): 예상되는 주요 이벤트와 변화
                    - 중기 전망 (3개월): 지속적으로 모니터링해야 할 트렌드
                    - Action Items: 실무자가 취해야 할 구체적인 액션 (2-3개)

                    작성 가이드라인:
                    - 구체적인 사실, 숫자, 인용을 활용하여 객관성 확보
                    - 마케팅성 과장 표현 지양, 분석적 관점 유지
                    - "~것으로 보인다" 대신 "~한다"로 단정적 서술
                    - 실무자가 바로 활용 가능한 actionable insight 제공
                    - 한국어로 작성, 전문 용어는 영문 병기
                    """,
                    targetDate.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")),
                    articles.size(),
                    clusters.size(),
                    trendAnalysis.emergingTopics.isEmpty() ? "없음" : String.join(", ", trendAnalysis.emergingTopics),
                    trendAnalysis.hotTopics.isEmpty() ? "없음" : String.join(", ", trendAnalysis.hotTopics),
                    trendAnalysis.decliningTopics.isEmpty() ? "없음" : String.join(", ", trendAnalysis.decliningTopics),
                    String.join("\n\n", topArticleSummaries)
            );

            String summary = callClaudeCLI(prompt);
            log.info("Claude AI Executive Summary 생성 완료: {}자", summary.length());
            return summary;

        } catch (Exception e) {
            log.error("Claude AI Executive Summary 생성 실패", e);
            return generateFallbackExecutiveSummary(articles, clusters);
        }
    }

    /**
     * Claude CLI 호출
     */
    private String callClaudeCLI(String prompt) throws Exception {
        int timeout = aiConfig.getClaudeCli().getTimeout();
        String claudeCommand = aiConfig.getClaudeCli().getCommand();

        ProcessBuilder pb = new ProcessBuilder(claudeCommand, "--headless");
        pb.redirectErrorStream(true);

        Process process = pb.start();

        // 프롬프트 전송
        try (OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8)) {
            writer.write(prompt);
            writer.flush();
        }

        // 타임아웃과 함께 응답 대기
        boolean finished = process.waitFor(timeout, TimeUnit.SECONDS);

        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Claude CLI 타임아웃: " + timeout + "초 초과");
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

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new RuntimeException("Claude CLI 실행 실패 (exit code: " + exitCode + ")");
        }

        return response.toString().trim();
    }

    /**
     * Fallback Executive Summary (AI 실패 시)
     * - Railway 프로덕션 환경에서 사용됨
     * - A4 절반 분량 (~1500자)의 구조화된 리포트
     */
    private String generateFallbackExecutiveSummary(List<NewsArticle> articles, List<TopicCluster> clusters) {
        StringBuilder summary = new StringBuilder();

        // 카테고리 분포 분석
        Map<String, Integer> categoryDist = articles.stream()
                .filter(a -> a.getCategory() != null)
                .collect(Collectors.groupingBy(
                        a -> a.getCategory().name(),
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));

        // === 1. 핵심 요약 ===
        summary.append("## 핵심 요약\n\n");
        summary.append(generateCoreSummary(articles, clusters, categoryDist));
        summary.append("\n\n");

        // === 2. 주요 토픽 심층 분석 (상위 5개) ===
        summary.append("## 주요 토픽 심층 분석\n\n");
        summary.append(generateTopicInsights(articles, clusters));
        summary.append("\n\n");

        // === 3. 트렌드 인사이트 ===
        summary.append("## 트렌드 인사이트\n\n");
        summary.append(generateTrendInsights(articles, categoryDist));
        summary.append("\n\n");

        // === 4. 카테고리별 분포 ===
        summary.append("## 카테고리별 분포\n\n");
        categoryDist.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(6)
                .forEach(entry -> {
                    String displayName = getCategoryDisplayName(entry.getKey());
                    summary.append(String.format("- **%s**: %d건 (%.1f%%)\n",
                            displayName,
                            entry.getValue(),
                            (entry.getValue() * 100.0) / articles.size()));
                });

        summary.append("\n\n");

        // === 5. 향후 전망 ===
        summary.append("## 향후 전망\n\n");
        summary.append(generateOutlook(categoryDist, clusters));

        return summary.toString();
    }

    /**
     * 핵심 요약 생성 (2-3문장)
     */
    private String generateCoreSummary(List<NewsArticle> articles, List<TopicCluster> clusters, Map<String, Integer> categoryDist) {
        StringBuilder core = new StringBuilder();

        // 전체 통계
        core.append(String.format("최근 30일간 **%d개**의 중요 AI 뉴스가 수집되었으며, ", articles.size()));
        core.append(String.format("**%d개**의 주요 토픽이 식별되었습니다. ", clusters.size()));

        // 상위 카테고리
        if (!categoryDist.isEmpty()) {
            Map.Entry<String, Integer> topCategory = categoryDist.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .orElse(null);

            if (topCategory != null) {
                String displayName = getCategoryDisplayName(topCategory.getKey());
                core.append(String.format("**%s** 분야가 %d건으로 가장 활발한 움직임을 보였으며, ",
                        displayName, topCategory.getValue()));
            }
        }

        // 높은 관련성 기사 비율
        long highRelevanceCount = articles.stream()
                .filter(a -> a.getRelevanceScore() != null && a.getRelevanceScore() >= 0.8)
                .count();

        if (highRelevanceCount > 0) {
            double percentage = (highRelevanceCount * 100.0) / articles.size();
            core.append(String.format("전체 기사 중 %.0f%%가 높은 AI 관련성(0.8 이상)을 기록했습니다.", percentage));
        } else {
            core.append("다양한 AI 관련 이슈가 포괄적으로 다뤄지고 있습니다.");
        }

        return core.toString();
    }

    /**
     * 상위 5개 토픽 심층 분석
     */
    private String generateTopicInsights(List<NewsArticle> articles, List<TopicCluster> clusters) {
        StringBuilder insights = new StringBuilder();

        for (int i = 0; i < Math.min(clusters.size(), 5); i++) {
            TopicCluster cluster = clusters.get(i);
            insights.append(String.format("### %d. %s (%d건)\n\n",
                    i + 1, cluster.getTopicName(), cluster.getArticles().size()));

            // 카테고리 분포
            Map<String, Long> clusterCategories = cluster.getArticles().stream()
                    .filter(a -> a.getCategory() != null)
                    .collect(Collectors.groupingBy(
                            a -> a.getCategory().name(),
                            Collectors.counting()
                    ));

            if (!clusterCategories.isEmpty()) {
                String mainCategory = clusterCategories.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse("기타");

                insights.append(generateTopicCategoryDescription(mainCategory, cluster.getArticles().size()));
                insights.append("\n\n");
            }

            // 주요 기사 (관련성 점수 순)
            List<NewsArticle> topArticles = cluster.getArticles().stream()
                    .sorted((a, b) -> Double.compare(
                            b.getRelevanceScore() != null ? b.getRelevanceScore() : 0,
                            a.getRelevanceScore() != null ? a.getRelevanceScore() : 0
                    ))
                    .limit(3)
                    .collect(Collectors.toList());

            if (!topArticles.isEmpty()) {
                insights.append("**주요 기사**:\n");
                topArticles.forEach(article -> {
                    String title = article.getTitleKo() != null ? article.getTitleKo() : article.getTitle();
                    String displayTitle = title.length() > 70 ? title.substring(0, 70) + "..." : title;

                    if (article.getOriginalUrl() != null && !article.getOriginalUrl().isEmpty()) {
                        insights.append(String.format("- [%s](%s)\n", displayTitle, article.getOriginalUrl()));
                    } else {
                        insights.append(String.format("- %s\n", displayTitle));
                    }
                });
                insights.append("\n");
            }
        }

        return insights.toString();
    }

    /**
     * 토픽 카테고리별 맥락 설명
     */
    private String generateTopicCategoryDescription(String categoryKey, int articleCount) {
        return switch (categoryKey) {
            case "LLM" -> String.format(
                    "대규모 언어 모델 분야에서 %d건의 혁신이 보고되었습니다. " +
                            "GPT, Claude, Gemini 등 주요 모델의 성능 향상과 멀티모달 기능 강화가 두드러지며, " +
                            "엔터프라이즈 AI 적용이 본격화되고 있습니다.",
                    articleCount
            );
            case "COMPUTER_VISION" -> String.format(
                    "컴퓨터 비전 기술 %d건의 발전이 확인되었습니다. " +
                            "실시간 객체 인식, 3D 재구성, 의료 영상 분석 등 실용적 응용이 확대되고 있습니다.",
                    articleCount
            );
            case "NLP" -> String.format(
                    "자연어 처리 분야 %d건의 연구 성과가 발표되었습니다. " +
                            "번역, 요약, 감정 분석 등 전통적 NLP 태스크의 성능이 지속적으로 향상되고 있습니다.",
                    articleCount
            );
            case "INDUSTRY" -> String.format(
                    "AI 산업 동향 %d건이 보도되었습니다. " +
                            "투자, 인수합병, 전략적 파트너십 등 산업 재편이 활발하게 진행되고 있습니다.",
                    articleCount
            );
            case "REGULATION" -> String.format(
                    "AI 규제 및 정책 %d건이 발표되었습니다. " +
                            "EU AI Act, 미국 행정명령 등 글로벌 규제 프레임워크가 구체화되고 있습니다.",
                    articleCount
            );
            case "RESEARCH" -> String.format(
                    "AI 연구 %d건이 발표되었습니다. " +
                            "새로운 아키텍처, 학습 알고리즘, 평가 방법론 등 이론적 발전이 이루어지고 있습니다.",
                    articleCount
            );
            default -> String.format(
                    "해당 분야에서 %d건의 주요 발표가 있었습니다. " +
                            "AI 기술의 다양한 응용과 발전이 지속되고 있습니다.",
                    articleCount
            );
        };
    }

    /**
     * 트렌드 인사이트 생성
     */
    private String generateTrendInsights(List<NewsArticle> articles, Map<String, Integer> categoryDist) {
        StringBuilder trends = new StringBuilder();

        // 카테고리 다양성 분석
        int uniqueCategories = categoryDist.size();
        trends.append(String.format("- **카테고리 다양성**: %d개 분야에서 뉴스가 발생하여 " +
                "AI 생태계의 다각적 성장을 보여줍니다.\n\n", uniqueCategories));

        // 평균 관련성 점수
        double avgRelevance = articles.stream()
                .filter(a -> a.getRelevanceScore() != null)
                .mapToDouble(NewsArticle::getRelevanceScore)
                .average()
                .orElse(0.0);

        trends.append(String.format("- **AI 관련성**: 평균 %.2f점으로, ", avgRelevance));
        if (avgRelevance >= 0.85) {
            trends.append("핵심 기술 중심의 높은 품질 뉴스가 수집되었습니다.\n\n");
        } else if (avgRelevance >= 0.7) {
            trends.append("AI 관련성이 높은 유의미한 뉴스가 수집되었습니다.\n\n");
        } else {
            trends.append("다양한 관점에서 AI 이슈가 다뤄지고 있습니다.\n\n");
        }

        // 핵심 키워드 트렌드
        trends.append("- **핵심 키워드**: ");
        List<String> keywords = extractMeaningfulKeywords(articles);
        if (!keywords.isEmpty()) {
            trends.append(String.join(", ", keywords))
                    .append(" 등이 주요 화두입니다.\n\n");
        } else {
            trends.append("다양한 기술 키워드가 고르게 분포되어 있습니다.\n\n");
        }

        return trends.toString();
    }

    /**
     * 의미 있는 키워드 추출 (불용어 제외)
     */
    private List<String> extractMeaningfulKeywords(List<NewsArticle> articles) {
        // 확장된 불용어 목록
        Set<String> stopWords = Set.of(
                "ai", "인공지능", "개발", "발표", "출시", "공개", "새로운", "최신", "기술", "시스템",
                "서비스", "플랫폼", "솔루션", "기업", "회사", "국내", "글로벌", "연구", "분석",
                "이", "가", "을", "를", "의", "에", "와", "과", "도", "로", "으로", "는", "은",
                "위한", "통해", "대한", "있는", "있다", "한다", "된다", "한", "등", "및", "또는",
                "것으로", "이는", "있습니다", "됩니다", "하는", "있으며", "모델", "것이", "하며",
                "수", "등을", "것", "이다", "위해", "따른", "관련", "중", "더", "그", "매우"
        );

        Map<String, Integer> keywordFreq = new HashMap<>();

        for (NewsArticle article : articles) {
            String text = (article.getTitleKo() != null ? article.getTitleKo() : article.getTitle()) + " " +
                    (article.getSummary() != null ? article.getSummary() : "");

            // 단어 분리 (공백, 구두점, 괄호 등)
            String[] words = text.split("[\\s,\\.\\-\\(\\)\\[\\]\"']+");

            for (String word : words) {
                String clean = word.trim().toLowerCase();

                // 필터링: 길이, 불용어, 숫자 포함, 단일 문자
                if (clean.length() >= 2 && clean.length() <= 20 &&
                        !stopWords.contains(clean) &&
                        !clean.matches(".*[0-9]+.*") &&
                        !clean.matches("^[a-zA-Z가-힣]$")) {

                    keywordFreq.put(clean, keywordFreq.getOrDefault(clean, 0) + 1);
                }
            }
        }

        // 빈도수 3회 이상, 상위 5개 선정
        return keywordFreq.entrySet().stream()
                .filter(e -> e.getValue() >= 3)
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * 향후 전망 생성
     */
    private String generateOutlook(Map<String, Integer> categoryDist, List<TopicCluster> clusters) {
        StringBuilder outlook = new StringBuilder();

        // 가장 활발한 분야 기반 전망
        if (!categoryDist.isEmpty()) {
            Map.Entry<String, Integer> topCategory = categoryDist.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .orElse(null);

            if (topCategory != null) {
                String displayName = getCategoryDisplayName(topCategory.getKey());
                outlook.append(String.format("**%s** 분야의 활발한 움직임은 ", displayName));

                switch (topCategory.getKey()) {
                    case "LLM" -> outlook.append("향후 GPT-5, Claude 4 등 차세대 모델 출시와 관련된 " +
                            "추가 발표가 예상되며, 멀티모달 AI의 상용화가 본격화될 전망입니다.");
                    case "COMPUTER_VISION" -> outlook.append("자율주행과 로보틱스 분야의 실용화가 가속화되고, " +
                            "산업 현장에서의 AI 비전 시스템 도입이 확대될 것으로 보입니다.");
                    case "REGULATION" -> outlook.append("글로벌 AI 규제 구체화로 기업들의 컴플라이언스 대응이 " +
                            "핵심 이슈가 되며, 규제 준수 솔루션 시장이 성장할 것으로 예상됩니다.");
                    case "INDUSTRY" -> outlook.append("AI 산업 재편이 계속되며, 인수합병과 전략적 파트너십이 " +
                            "더욱 활발해질 전망입니다.");
                    default -> outlook.append("해당 분야의 지속적 발전과 새로운 혁신이 기대됩니다.");
                }

                outlook.append("\n\n");
            }
        }

        // 종합 전망
        outlook.append("AI 기술은 빠르게 진화하고 있으며, 특히 실용적 응용과 산업 적용이 가속화되고 있습니다. " +
                "향후 주요 기업들의 제품 발표와 학계의 연구 성과가 예정되어 있어, " +
                "AI 생태계 전반의 역동적 변화가 계속될 것으로 예상됩니다.");

        return outlook.toString();
    }

    /**
     * 카테고리 한글명 반환
     */
    private String getCategoryDisplayName(String categoryKey) {
        return switch (categoryKey) {
            case "LLM" -> "대규모 언어 모델";
            case "COMPUTER_VISION" -> "컴퓨터 비전";
            case "NLP" -> "자연어 처리";
            case "ROBOTICS" -> "로보틱스";
            case "ML_OPS" -> "ML Ops";
            case "RESEARCH" -> "연구/논문";
            case "INDUSTRY" -> "산업 동향";
            case "STARTUP" -> "스타트업";
            case "REGULATION" -> "규제/정책";
            case "TUTORIAL" -> "튜토리얼";
            case "PRODUCT" -> "제품/서비스";
            default -> "기타";
        };
    }

    /**
     * 토픽별 AI 요약 생성 (각 토픽당 2-3문장)
     */
    private String generateAITopicSummaries(List<TopicCluster> clusters) {
        List<Map<String, Object>> summaries = new ArrayList<>();

        for (int i = 0; i < Math.min(clusters.size(), 5); i++) {
            TopicCluster cluster = clusters.get(i);

            Map<String, Object> summary = new HashMap<>();
            summary.put("topic", cluster.getTopicName());
            summary.put("articleCount", cluster.getArticles().size());

            // 대표 기사 제목 목록
            List<String> titles = cluster.getArticles().stream()
                    .limit(3)
                    .map(article -> article.getTitleKo() != null ? article.getTitleKo() : article.getTitle())
                    .collect(Collectors.toList());
            summary.put("representativeTitles", titles);

            // 카테고리 분포
            Map<String, Integer> categoryDist = cluster.getArticles().stream()
                    .filter(a -> a.getCategory() != null)
                    .collect(Collectors.groupingBy(
                            a -> a.getCategory().name(),
                            Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                    ));
            summary.put("categories", categoryDist);

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
     * TF-IDF 기반 키워드 추출
     */
    private String extractTFIDFKeyTrends(List<NewsArticle> articles) {
        // 간단한 키워드 빈도 분석 (TF-IDF는 전체 코퍼스 필요)
        Map<String, Integer> keywordFrequency = new HashMap<>();

        for (NewsArticle article : articles) {
            String title = article.getTitleKo() != null ? article.getTitleKo() : article.getTitle();
            if (title != null) {
                String[] words = title.split("\\s+");
                for (String word : words) {
                    if (word.length() > 2 && !isStopWord(word)) {
                        keywordFrequency.put(word, keywordFrequency.getOrDefault(word, 0) + 1);
                    }
                }
            }
        }

        List<Map<String, Object>> trends = keywordFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(15)
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
     * 불용어 체크 (간단 버전)
     */
    private boolean isStopWord(String word) {
        Set<String> stopWords = Set.of("the", "is", "at", "which", "on", "a", "an", "and", "or", "but",
                "이", "그", "저", "것", "수", "등", "및", "를", "을", "가", "에", "의", "와");
        return stopWords.contains(word.toLowerCase());
    }

    /**
     * 카테고리 분포 계산
     */
    private String calculateCategoryDistribution(List<NewsArticle> articles) {
        Map<String, Integer> distribution = articles.stream()
                .filter(a -> a.getCategory() != null)
                .collect(Collectors.groupingBy(
                        a -> a.getCategory().name(),
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));

        try {
            return objectMapper.writeValueAsString(distribution);
        } catch (JsonProcessingException e) {
            log.error("카테고리 분포 JSON 변환 실패", e);
            return "{}";
        }
    }

    /**
     * 평균 관련성 점수 계산
     */
    private double calculateAvgRelevanceScore(List<NewsArticle> articles) {
        return articles.stream()
                .filter(a -> a.getRelevanceScore() != null)
                .mapToDouble(NewsArticle::getRelevanceScore)
                .average()
                .orElse(0.0);
    }

    /**
     * 리포트 품질 점수 계산
     * - 기사 품질, 다양성, 클러스터 품질 고려
     */
    private double calculateReportQualityScore(List<NewsArticle> articles, List<TopicCluster> clusters) {
        double score = 0.0;

        // 1. 기사 수 (30% 가중치)
        double articleScore = Math.min(articles.size() / 30.0, 1.0) * 0.3;

        // 2. 클러스터 다양성 (30% 가중치)
        double diversityScore = Math.min(clusters.size() / 8.0, 1.0) * 0.3;

        // 3. 평균 관련성 점수 (40% 가중치)
        double relevanceScore = calculateAvgRelevanceScore(articles) * 0.4;

        score = articleScore + diversityScore + relevanceScore;

        return Math.min(score, 1.0);
    }

    /**
     * 토픽 클러스터 직렬화
     */
    private String serializeTopicClusters(List<TopicCluster> clusters) {
        List<Map<String, Object>> serialized = clusters.stream()
                .map(cluster -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("topic", cluster.getTopicName());
                    map.put("articleCount", cluster.getArticles().size());
                    map.put("articleIds", cluster.getArticles().stream()
                            .map(NewsArticle::getId)
                            .collect(Collectors.toList()));
                    return map;
                })
                .collect(Collectors.toList());

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
     * 트렌드 분석 결과
     */
    private static class TrendAnalysis {
        List<String> emergingTopics = new ArrayList<>();     // 신규 등장
        List<String> hotTopics = new ArrayList<>();          // 급증
        List<String> decliningTopics = new ArrayList<>();    // 감소
        List<String> stableTopics = new ArrayList<>();       // 안정적
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
