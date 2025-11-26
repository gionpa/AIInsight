package com.aiinsight.domain.crawl;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "crawl_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrawlHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_id", nullable = false)
    private CrawlTarget target;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CrawlTarget.CrawlStatus status;

    // 발견된 총 기사 수
    @Column(name = "articles_found")
    @Builder.Default
    private Integer articlesFound = 0;

    // 새로 추가된 기사 수
    @Column(name = "articles_new")
    @Builder.Default
    private Integer articlesNew = 0;

    // 실행 시간 (밀리초)
    @Column(name = "duration_ms")
    private Long durationMs;

    // 에러 메시지 (실패 시)
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "executed_at", nullable = false, updatable = false)
    private LocalDateTime executedAt;
}
