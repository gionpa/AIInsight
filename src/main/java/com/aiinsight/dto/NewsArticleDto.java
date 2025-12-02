package com.aiinsight.dto;

import com.aiinsight.domain.article.NewsArticle;
import lombok.*;

import java.time.LocalDateTime;

public class NewsArticleDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private Long targetId;
        private String targetName;
        private String originalUrl;
        private String title;
        private String titleKo;
        private String summary;
        private String author;
        private LocalDateTime publishedAt;
        private Double relevanceScore;
        private NewsArticle.ArticleCategory category;
        private NewsArticle.ArticleImportance importance;
        private Boolean isNew;
        private Boolean isSummarized;
        private String thumbnailUrl;
        private LocalDateTime crawledAt;

        public static Response from(NewsArticle entity) {
            // 1시간 이내 크롤링된 기사만 NEW 표시
            boolean isNewArticle = entity.getCrawledAt() != null
                    && entity.getCrawledAt().isAfter(LocalDateTime.now().minusHours(1));

            return Response.builder()
                    .id(entity.getId())
                    .targetId(entity.getTarget().getId())
                    .targetName(entity.getTarget().getName())
                    .originalUrl(entity.getOriginalUrl())
                    .title(entity.getTitle())
                    .titleKo(entity.getTitleKo())
                    .summary(entity.getSummary())
                    .author(entity.getAuthor())
                    .publishedAt(entity.getPublishedAt())
                    .relevanceScore(entity.getRelevanceScore())
                    .category(entity.getCategory())
                    .importance(entity.getImportance())
                    .isNew(isNewArticle)
                    .isSummarized(entity.getIsSummarized())
                    .thumbnailUrl(entity.getThumbnailUrl())
                    .crawledAt(entity.getCrawledAt())
                    .build();
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DetailResponse {
        private Long id;
        private Long targetId;
        private String targetName;
        private String originalUrl;
        private String title;
        private String titleKo;
        private String content;
        private String summary;
        private String author;
        private LocalDateTime publishedAt;
        private Double relevanceScore;
        private NewsArticle.ArticleCategory category;
        private NewsArticle.ArticleImportance importance;
        private Boolean isNew;
        private Boolean isSummarized;
        private String thumbnailUrl;
        private LocalDateTime crawledAt;
        private LocalDateTime updatedAt;

        public static DetailResponse from(NewsArticle entity) {
            // 1시간 이내 크롤링된 기사만 NEW 표시
            boolean isNewArticle = entity.getCrawledAt() != null
                    && entity.getCrawledAt().isAfter(LocalDateTime.now().minusHours(1));

            return DetailResponse.builder()
                    .id(entity.getId())
                    .targetId(entity.getTarget().getId())
                    .targetName(entity.getTarget().getName())
                    .originalUrl(entity.getOriginalUrl())
                    .title(entity.getTitle())
                    .titleKo(entity.getTitleKo())
                    .content(entity.getContent())
                    .summary(entity.getSummary())
                    .author(entity.getAuthor())
                    .publishedAt(entity.getPublishedAt())
                    .relevanceScore(entity.getRelevanceScore())
                    .category(entity.getCategory())
                    .importance(entity.getImportance())
                    .isNew(isNewArticle)
                    .isSummarized(entity.getIsSummarized())
                    .thumbnailUrl(entity.getThumbnailUrl())
                    .crawledAt(entity.getCrawledAt())
                    .updatedAt(entity.getUpdatedAt())
                    .build();
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SearchRequest {
        private String keyword;
        private NewsArticle.ArticleCategory category;
        private NewsArticle.ArticleImportance importance;
        private Double minRelevanceScore;
        private Boolean isNew;
        private Long targetId;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
    }
}
