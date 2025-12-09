package com.aiinsight.dto;

import com.aiinsight.domain.report.DailyReport;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 일일 리포트 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyReportResponse {

    private Long id;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate reportDate;

    private String executiveSummary;

    private List<KeyTrend> keyTrends;

    private List<TopicSummary> topicSummaries;

    private Integer totalArticles;

    private Integer highImportanceArticles;

    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime generatedAt;

    /**
     * Entity → DTO 변환
     */
    public static DailyReportResponse fromEntity(DailyReport report) {
        return DailyReportResponse.builder()
                .id(report.getId())
                .reportDate(report.getReportDate())
                .executiveSummary(report.getExecutiveSummary())
                .keyTrends(KeyTrend.parseJson(report.getKeyTrends()))
                .topicSummaries(TopicSummary.parseJson(report.getTopicSummaries()))
                .totalArticles(report.getTotalArticles())
                .highImportanceArticles(report.getHighImportanceArticles())
                .status(report.getStatus().name())
                .generatedAt(report.getCreatedAt())
                .build();
    }

    /**
     * 키 트렌드 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KeyTrend {
        private String keyword;
        private Integer frequency;

        public static List<KeyTrend> parseJson(String json) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.core.type.TypeReference<List<KeyTrend>> typeRef =
                        new com.fasterxml.jackson.core.type.TypeReference<List<KeyTrend>>() {};
                return mapper.readValue(json, typeRef);
            } catch (Exception e) {
                return List.of();
            }
        }
    }

    /**
     * 토픽 요약 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopicSummary {
        private String topic;
        private Integer articleCount;
        private List<String> representativeTitles;

        public static List<TopicSummary> parseJson(String json) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.core.type.TypeReference<List<TopicSummary>> typeRef =
                        new com.fasterxml.jackson.core.type.TypeReference<List<TopicSummary>>() {};
                return mapper.readValue(json, typeRef);
            } catch (Exception e) {
                return List.of();
            }
        }
    }
}
