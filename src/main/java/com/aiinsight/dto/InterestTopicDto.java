package com.aiinsight.dto;

import com.aiinsight.domain.topic.InterestTopic;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class InterestTopicDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private String name;
        private String description;
        private String keywords;
        private Integer displayOrder;
        private Boolean isActive;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static Response from(InterestTopic entity) {
            return Response.builder()
                    .id(entity.getId())
                    .name(entity.getName())
                    .description(entity.getDescription())
                    .keywords(entity.getKeywords())
                    .displayOrder(entity.getDisplayOrder())
                    .isActive(entity.getIsActive())
                    .createdAt(entity.getCreatedAt())
                    .updatedAt(entity.getUpdatedAt())
                    .build();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateRequest {
        @NotBlank(message = "주제 이름은 필수입니다")
        @Size(max = 100, message = "주제 이름은 100자 이내여야 합니다")
        private String name;

        @Size(max = 500, message = "설명은 500자 이내여야 합니다")
        private String description;

        @NotBlank(message = "키워드는 필수입니다")
        @Size(max = 1000, message = "키워드는 1000자 이내여야 합니다")
        private String keywords;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateRequest {
        @Size(max = 100, message = "주제 이름은 100자 이내여야 합니다")
        private String name;

        @Size(max = 500, message = "설명은 500자 이내여야 합니다")
        private String description;

        @Size(max = 1000, message = "키워드는 1000자 이내여야 합니다")
        private String keywords;

        private Boolean isActive;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReorderRequest {
        private List<Long> topicIds;
    }

    /**
     * 관심 주제별 리포트 응답
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TopicReportResponse {
        private Long topicId;
        private String topicName;
        private String description;
        private String keywords;
        private Integer totalArticles;
        private Integer highImportanceCount;
        private List<NewsArticleDto.Response> articles;
    }

    /**
     * 전체 관심 주제 리포트 응답 (여러 주제 포함)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AllTopicsReportResponse {
        private LocalDateTime generatedAt;
        private Integer totalTopics;
        private Integer totalArticles;
        private List<TopicReportResponse> topics;
    }
}
