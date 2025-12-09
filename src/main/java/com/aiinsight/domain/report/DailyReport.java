package com.aiinsight.domain.report;

import com.aiinsight.domain.article.NewsArticle;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 일일 AI 뉴스 리포트 엔티티
 * - 임베딩 벡터 기반 유사도 분석으로 생성
 * - 주요 트렌드, 토픽, Executive Summary 포함
 */
@Entity
@Table(name = "daily_report", indexes = {
        @Index(name = "idx_daily_report_date", columnList = "report_date", unique = true),
        @Index(name = "idx_daily_report_status", columnList = "status"),
        @Index(name = "idx_daily_report_created_at", columnList = "created_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 리포트 날짜 (YYYY-MM-DD)
     */
    @Column(name = "report_date", nullable = false, unique = true)
    private LocalDate reportDate;

    /**
     * Executive Summary (전체 요약)
     * - 당일 주요 뉴스를 3-5문장으로 요약
     */
    @Column(name = "executive_summary", columnDefinition = "TEXT", nullable = false)
    private String executiveSummary;

    /**
     * 주요 트렌드 (JSON 형식)
     * - 키워드, 빈도, 중요도 등
     * - 예: [{"keyword": "GPT-5", "frequency": 15, "importance": 0.95}, ...]
     */
    @Column(name = "key_trends", columnDefinition = "TEXT")
    private String keyTrends;

    /**
     * 토픽별 요약 (JSON 형식)
     * - 토픽명, 요약, 관련 기사 수
     * - 예: [{"topic": "LLM 발전", "summary": "...", "articleCount": 12}, ...]
     */
    @Column(name = "topic_summaries", columnDefinition = "TEXT")
    private String topicSummaries;

    /**
     * 토픽 클러스터 정보 (JSON 형식)
     * - 임베딩 기반 클러스터링 결과
     * - 클러스터 ID, 중심 벡터, 기사 ID 목록
     */
    @Column(name = "topic_clusters", columnDefinition = "TEXT")
    private String topicClusters;

    /**
     * 포함된 기사 수
     */
    @Column(name = "total_articles", nullable = false)
    private Integer totalArticles;

    /**
     * 고중요도 기사 수
     */
    @Column(name = "high_importance_articles")
    private Integer highImportanceArticles;

    /**
     * 카테고리별 분포 (JSON 형식)
     * - 예: {"LLM": 25, "COMPUTER_VISION": 10, ...}
     */
    @Column(name = "category_distribution", columnDefinition = "TEXT")
    private String categoryDistribution;

    /**
     * 평균 AI 관련성 점수
     */
    @Column(name = "avg_relevance_score")
    private Double avgRelevanceScore;

    /**
     * 리포트 생성 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ReportStatus status = ReportStatus.PENDING;

    /**
     * 리포트 품질 점수 (0.0 ~ 1.0)
     * - 기사 품질, 다양성, 완전성 등을 고려
     */
    @Column(name = "quality_score")
    private Double qualityScore;

    /**
     * 리포트 생성에 사용된 모델
     */
    @Column(name = "generation_model", length = 100)
    private String generationModel;

    /**
     * 리포트 생성 소요 시간 (밀리초)
     */
    @Column(name = "generation_duration_ms")
    private Long generationDurationMs;

    /**
     * 생성 일시
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 수정 일시
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 리포트 생성 오류 메시지
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * 리포트에 포함된 기사 목록
     * - Many-to-Many 관계
     */
    @ManyToMany
    @JoinTable(
            name = "daily_report_articles",
            joinColumns = @JoinColumn(name = "report_id"),
            inverseJoinColumns = @JoinColumn(name = "article_id"),
            indexes = {
                    @Index(name = "idx_report_articles_report_id", columnList = "report_id"),
                    @Index(name = "idx_report_articles_article_id", columnList = "article_id")
            }
    )
    @Builder.Default
    private List<NewsArticle> articles = new ArrayList<>();

    /**
     * 리포트 상태
     */
    public enum ReportStatus {
        PENDING,      // 생성 대기 중
        PROCESSING,   // 생성 중
        COMPLETED,    // 생성 완료
        FAILED        // 생성 실패
    }

    /**
     * 기사 추가
     */
    public void addArticle(NewsArticle article) {
        this.articles.add(article);
    }

    /**
     * 기사 목록 설정
     */
    public void setArticles(List<NewsArticle> articles) {
        this.articles.clear();
        if (articles != null) {
            this.articles.addAll(articles);
        }
    }
}
