package com.aiinsight.domain.article;

import com.aiinsight.domain.crawl.CrawlTarget;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "news_article", indexes = {
    @Index(name = "idx_article_hash", columnList = "content_hash"),
    @Index(name = "idx_article_published", columnList = "published_at"),
    @Index(name = "idx_article_category", columnList = "category")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_id", nullable = false)
    private CrawlTarget target;

    @Column(name = "original_url", nullable = false, length = 2048)
    private String originalUrl;

    @Column(nullable = false, length = 500)
    private String title;

    // 한글 번역 제목
    @Column(name = "title_ko", length = 500)
    private String titleKo;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String summary;

    // 원문 작성자
    private String author;

    // 원문 게시일
    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    // AI 분석 관련성 점수 (0.0 ~ 1.0)
    @Column(name = "relevance_score")
    private Double relevanceScore;

    // 카테고리 (AI가 분류)
    @Enumerated(EnumType.STRING)
    private ArticleCategory category;

    // 중요도 (AI가 판단)
    @Enumerated(EnumType.STRING)
    private ArticleImportance importance;

    // 중복 방지용 해시 (URL + 제목 기반)
    @Column(name = "content_hash", nullable = false, unique = true, length = 64)
    private String contentHash;

    // 신규 여부 (아직 사용자에게 노출되지 않은 경우)
    @Column(name = "is_new")
    @Builder.Default
    private Boolean isNew = true;

    // 요약 처리 완료 여부
    @Column(name = "is_summarized")
    @Builder.Default
    private Boolean isSummarized = false;

    // 썸네일 이미지 URL
    @Column(name = "thumbnail_url", length = 2048)
    private String thumbnailUrl;

    @CreationTimestamp
    @Column(name = "crawled_at", nullable = false, updatable = false)
    private LocalDateTime crawledAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum ArticleCategory {
        LLM,                // Large Language Models
        COMPUTER_VISION,    // 컴퓨터 비전
        NLP,                // 자연어 처리
        ROBOTICS,           // 로보틱스
        ML_OPS,             // MLOps
        RESEARCH,           // 연구/논문
        INDUSTRY,           // 산업 동향
        STARTUP,            // 스타트업 소식
        REGULATION,         // 규제/정책
        TUTORIAL,           // 튜토리얼/가이드
        PRODUCT,            // 제품 출시
        OTHER               // 기타
    }

    public enum ArticleImportance {
        HIGH,       // 중요
        MEDIUM,     // 보통
        LOW         // 낮음
    }
}
