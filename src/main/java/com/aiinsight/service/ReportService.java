package com.aiinsight.service;

import com.aiinsight.domain.article.NewsArticle;
import com.aiinsight.domain.article.NewsArticleRepository;
import com.aiinsight.dto.ReportDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReportService {

    private final NewsArticleRepository newsArticleRepository;

    private static final Map<String, String> CATEGORY_DISPLAY_NAMES = Map.ofEntries(
            Map.entry("LLM", "대규모 언어 모델"),
            Map.entry("COMPUTER_VISION", "컴퓨터 비전"),
            Map.entry("NLP", "자연어 처리"),
            Map.entry("ROBOTICS", "로보틱스"),
            Map.entry("ML_OPS", "ML Ops"),
            Map.entry("RESEARCH", "연구/논문"),
            Map.entry("INDUSTRY", "산업 동향"),
            Map.entry("STARTUP", "스타트업"),
            Map.entry("REGULATION", "규제/정책"),
            Map.entry("TUTORIAL", "튜토리얼"),
            Map.entry("PRODUCT", "제품/서비스"),
            Map.entry("OTHER", "기타")
    );

    /**
     * 오늘의 HIGH 중요도 기사 리포트 생성
     */
    public ReportDto.DailyReport generateDailyReport() {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();

        // HIGH 중요도 기사 조회
        Page<NewsArticle> highImportanceArticles = newsArticleRepository
                .findByImportanceOrderByCrawledAtDesc(
                        NewsArticle.ArticleImportance.HIGH,
                        PageRequest.of(0, 50)
                );

        List<NewsArticle> articles = highImportanceArticles.getContent();

        // 카테고리별 분포
        Map<String, Integer> categoryDistribution = articles.stream()
                .filter(a -> a.getCategory() != null)
                .collect(Collectors.groupingBy(
                        a -> a.getCategory().name(),
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));

        // 기사 요약 목록 생성
        List<ReportDto.ArticleSummary> articleSummaries = articles.stream()
                .map(ReportDto.ArticleSummary::from)
                .collect(Collectors.toList());

        // Executive Summary 생성
        String executiveSummary = generateExecutiveSummary(articles, categoryDistribution);

        return ReportDto.DailyReport.builder()
                .generatedAt(LocalDateTime.now())
                .period(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .totalHighImportanceArticles(articles.size())
                .categoryDistribution(categoryDistribution)
                .articles(articleSummaries)
                .executiveSummary(executiveSummary)
                .build();
    }

    /**
     * 카테고리별 리포트 생성
     */
    public List<ReportDto.CategoryReport> generateCategoryReport() {
        Page<NewsArticle> highImportanceArticles = newsArticleRepository
                .findByImportanceOrderByCrawledAtDesc(
                        NewsArticle.ArticleImportance.HIGH,
                        PageRequest.of(0, 100)
                );

        // 카테고리별로 그룹화
        Map<NewsArticle.ArticleCategory, List<NewsArticle>> byCategory = highImportanceArticles.getContent()
                .stream()
                .filter(a -> a.getCategory() != null)
                .collect(Collectors.groupingBy(NewsArticle::getCategory));

        List<ReportDto.CategoryReport> reports = new ArrayList<>();

        for (Map.Entry<NewsArticle.ArticleCategory, List<NewsArticle>> entry : byCategory.entrySet()) {
            String categoryName = entry.getKey().name();
            List<ReportDto.ArticleSummary> summaries = entry.getValue().stream()
                    .sorted((a, b) -> Double.compare(
                            b.getRelevanceScore() != null ? b.getRelevanceScore() : 0,
                            a.getRelevanceScore() != null ? a.getRelevanceScore() : 0
                    ))
                    .map(ReportDto.ArticleSummary::from)
                    .collect(Collectors.toList());

            reports.add(ReportDto.CategoryReport.builder()
                    .category(categoryName)
                    .categoryDisplayName(CATEGORY_DISPLAY_NAMES.getOrDefault(categoryName, categoryName))
                    .articleCount(summaries.size())
                    .articles(summaries)
                    .build());
        }

        // 기사 수가 많은 순으로 정렬
        reports.sort((a, b) -> Integer.compare(b.getArticleCount(), a.getArticleCount()));

        return reports;
    }

    /**
     * 특정 카테고리의 HIGH 중요도 기사 조회
     */
    public ReportDto.CategoryReport getReportByCategory(String category) {
        NewsArticle.ArticleCategory articleCategory;
        try {
            articleCategory = NewsArticle.ArticleCategory.valueOf(category.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 카테고리: " + category);
        }

        Page<NewsArticle> articles = newsArticleRepository
                .findByCategoryAndImportanceOrderByRelevanceScoreDesc(
                        articleCategory,
                        NewsArticle.ArticleImportance.HIGH,
                        PageRequest.of(0, 50)
                );

        List<ReportDto.ArticleSummary> summaries = articles.getContent().stream()
                .map(ReportDto.ArticleSummary::from)
                .collect(Collectors.toList());

        return ReportDto.CategoryReport.builder()
                .category(category.toUpperCase())
                .categoryDisplayName(CATEGORY_DISPLAY_NAMES.getOrDefault(category.toUpperCase(), category))
                .articleCount(summaries.size())
                .articles(summaries)
                .build();
    }

    /**
     * Executive Summary 자동 생성
     */
    private String generateExecutiveSummary(List<NewsArticle> articles, Map<String, Integer> categoryDistribution) {
        if (articles.isEmpty()) {
            return "오늘 수집된 중요 AI 뉴스가 없습니다.";
        }

        StringBuilder summary = new StringBuilder();
        summary.append(String.format("오늘 총 %d개의 중요 AI 뉴스가 수집되었습니다. ", articles.size()));

        // 가장 많은 카테고리 찾기
        Optional<Map.Entry<String, Integer>> topCategory = categoryDistribution.entrySet().stream()
                .max(Map.Entry.comparingByValue());

        if (topCategory.isPresent()) {
            String catName = CATEGORY_DISPLAY_NAMES.getOrDefault(topCategory.get().getKey(), topCategory.get().getKey());
            summary.append(String.format("가장 많은 뉴스가 발생한 분야는 '%s' (%d건)입니다. ",
                    catName, topCategory.get().getValue()));
        }

        // 관련성 점수가 높은 상위 기사 언급
        articles.stream()
                .filter(a -> a.getRelevanceScore() != null && a.getRelevanceScore() >= 0.9)
                .findFirst()
                .ifPresent(article -> {
                    summary.append(String.format("특히 주목할 만한 뉴스로 \"%s\"가 있습니다.",
                            article.getTitle().length() > 50 ? article.getTitle().substring(0, 50) + "..." : article.getTitle()));
                });

        return summary.toString();
    }
}
