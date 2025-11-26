package com.aiinsight.dto;

import com.aiinsight.domain.crawl.CrawlTarget;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.time.LocalDateTime;

public class CrawlTargetDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateRequest {
        @NotBlank(message = "이름은 필수입니다")
        private String name;

        @NotBlank(message = "URL은 필수입니다")
        @Pattern(regexp = "^https?://.*", message = "올바른 URL 형식이 아닙니다")
        private String url;

        private String description;

        private String selectorConfig;

        @NotBlank(message = "Cron 표현식은 필수입니다")
        private String cronExpression;

        private CrawlTarget.CrawlType crawlType;

        private Boolean enabled;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateRequest {
        private String name;
        private String url;
        private String description;
        private String selectorConfig;
        private String cronExpression;
        private CrawlTarget.CrawlType crawlType;
        private Boolean enabled;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private String name;
        private String url;
        private String description;
        private String selectorConfig;
        private String cronExpression;
        private Boolean enabled;
        private CrawlTarget.CrawlType crawlType;
        private LocalDateTime lastCrawledAt;
        private CrawlTarget.CrawlStatus lastStatus;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static Response from(CrawlTarget entity) {
            return Response.builder()
                    .id(entity.getId())
                    .name(entity.getName())
                    .url(entity.getUrl())
                    .description(entity.getDescription())
                    .selectorConfig(entity.getSelectorConfig())
                    .cronExpression(entity.getCronExpression())
                    .enabled(entity.getEnabled())
                    .crawlType(entity.getCrawlType())
                    .lastCrawledAt(entity.getLastCrawledAt())
                    .lastStatus(entity.getLastStatus())
                    .createdAt(entity.getCreatedAt())
                    .updatedAt(entity.getUpdatedAt())
                    .build();
        }
    }
}
