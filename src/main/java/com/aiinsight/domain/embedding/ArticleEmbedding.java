package com.aiinsight.domain.embedding;

import com.aiinsight.domain.article.NewsArticle;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 기사 임베딩 벡터 엔티티
 * - pgvector를 사용한 의미 기반 유사도 검색
 * - 기본 모델: BAAI/bge-m3 (1024 차원, text-embeddings-inference 호환)
 */
@Entity
@Table(name = "article_embedding", indexes = {
        @Index(name = "idx_article_embedding_article_id", columnList = "article_id"),
        @Index(name = "idx_article_embedding_created_at", columnList = "created_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleEmbedding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 연관된 뉴스 기사
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false, unique = true)
    private NewsArticle article;

    /**
     * 임베딩 벡터 (기본 1024 차원)
     * - pgvector의 vector 타입 사용
     * - 코사인 유사도 검색 지원
     */
    @Column(name = "embedding_vector", columnDefinition = "vector(384)", nullable = false)
    @ColumnTransformer(write = "?::vector")
    private String embeddingVector;  // PostgreSQL vector 타입 (문자열로 저장)

    /**
     * 임베딩 모델 정보
     * - 예: "BAAI/bge-m3", "text-embedding-3-small"
     */
    @Column(name = "model_name", length = 100, nullable = false)
    private String modelName;

    /**
     * 임베딩 모델 버전
     */
    @Column(name = "model_version", length = 50)
    private String modelVersion;

    /**
     * 임베딩 생성에 사용된 텍스트 길이
     */
    @Column(name = "text_length")
    private Integer textLength;

    /**
     * 임베딩 생성 시 사용된 토큰 수
     */
    @Column(name = "token_count")
    private Integer tokenCount;

    /**
     * 임베딩 품질 점수 (0.0 ~ 1.0)
     * - 텍스트 품질, 길이 등을 고려한 신뢰도
     */
    @Column(name = "quality_score")
    private Double qualityScore;

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
     * 임베딩 재생성 여부
     */
    @Column(name = "is_regenerated")
    @Builder.Default
    private Boolean isRegenerated = false;

    /**
     * 재생성 사유
     */
    @Column(name = "regeneration_reason", length = 500)
    private String regenerationReason;
}
