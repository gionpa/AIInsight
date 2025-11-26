package com.aiinsight.crawler;

import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrawlResult {

    private boolean success;
    private String errorMessage;
    private long durationMs;

    @Builder.Default
    private List<ArticleData> articles = new ArrayList<>();

    public int getArticleCount() {
        return articles.size();
    }

    public void addArticle(ArticleData article) {
        articles.add(article);
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ArticleData {
        private String url;
        private String title;
        private String content;
        private String author;
        private LocalDateTime publishedAt;
        private String thumbnailUrl;
    }
}
