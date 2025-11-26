package com.aiinsight.dto;

import com.aiinsight.domain.article.NewsArticle;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class ReportDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyReport {
        private LocalDateTime generatedAt;
        private String period;
        private int totalHighImportanceArticles;
        private Map<String, Integer> categoryDistribution;
        private List<ArticleSummary> articles;
        private String executiveSummary;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ArticleSummary {
        private Long id;
        private String title;        // 한글 제목 (titleKo 우선, 없으면 원문 title)
        private String originalTitle; // 원문 제목
        private String summary;
        private String category;
        private Double relevanceScore;
        private String sourceName;
        private String originalUrl;
        private LocalDateTime crawledAt;

        public static ArticleSummary from(NewsArticle article) {
            // titleKo가 있으면 사용, 없으면 원문 title 사용
            String displayTitle = article.getTitleKo() != null && !article.getTitleKo().isEmpty()
                    ? article.getTitleKo()
                    : article.getTitle();

            return ArticleSummary.builder()
                    .id(article.getId())
                    .title(displayTitle)
                    .originalTitle(article.getTitle())
                    .summary(article.getSummary())
                    .category(article.getCategory() != null ? article.getCategory().name() : null)
                    .relevanceScore(article.getRelevanceScore())
                    .sourceName(article.getTarget() != null ? article.getTarget().getName() : null)
                    .originalUrl(article.getOriginalUrl())
                    .crawledAt(article.getCrawledAt())
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryReport {
        private String category;
        private String categoryDisplayName;
        private int articleCount;
        private List<ArticleSummary> articles;
    }
}
