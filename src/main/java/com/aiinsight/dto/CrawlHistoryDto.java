package com.aiinsight.dto;

import com.aiinsight.domain.crawl.CrawlHistory;
import com.aiinsight.domain.crawl.CrawlTarget;
import lombok.*;

import java.time.LocalDateTime;

public class CrawlHistoryDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private Long targetId;
        private String targetName;
        private CrawlTarget.CrawlStatus status;
        private Integer articlesFound;
        private Integer articlesNew;
        private Long durationMs;
        private String errorMessage;
        private LocalDateTime executedAt;

        public static Response from(CrawlHistory entity) {
            return Response.builder()
                    .id(entity.getId())
                    .targetId(entity.getTarget().getId())
                    .targetName(entity.getTarget().getName())
                    .status(entity.getStatus())
                    .articlesFound(entity.getArticlesFound())
                    .articlesNew(entity.getArticlesNew())
                    .durationMs(entity.getDurationMs())
                    .errorMessage(entity.getErrorMessage())
                    .executedAt(entity.getExecutedAt())
                    .build();
        }
    }
}
