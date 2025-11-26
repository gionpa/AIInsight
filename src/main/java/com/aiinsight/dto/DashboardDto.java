package com.aiinsight.dto;

import com.aiinsight.domain.article.NewsArticle;
import lombok.*;

import java.util.List;
import java.util.Map;

public class DashboardDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Stats {
        private Long totalTargets;
        private Long activeTargets;
        private Long totalArticles;
        private Long newArticles;
        private Long todayCrawled;
        private Long successfulCrawls;
        private Long failedCrawls;
        private Map<NewsArticle.ArticleCategory, Long> categoryDistribution;
        private List<RecentCrawl> recentCrawls;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecentCrawl {
        private Long targetId;
        private String targetName;
        private String status;
        private Integer articlesNew;
        private String executedAt;
    }
}
