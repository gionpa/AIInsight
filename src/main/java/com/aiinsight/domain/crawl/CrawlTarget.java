package com.aiinsight.domain.crawl;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "crawl_target")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrawlTarget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String url;

    @Column(columnDefinition = "TEXT")
    private String description;

    // CSS 선택자 설정 (JSON 형태로 저장)
    @Column(name = "selector_config", columnDefinition = "TEXT")
    private String selectorConfig;

    // 크롤링 주기 (Cron 표현식)
    @Column(name = "cron_expression", nullable = false)
    private String cronExpression;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CrawlType crawlType = CrawlType.STATIC;

    // 마지막 크롤링 시간
    @Column(name = "last_crawled_at")
    private LocalDateTime lastCrawledAt;

    // 마지막 크롤링 상태
    @Enumerated(EnumType.STRING)
    @Column(name = "last_status")
    private CrawlStatus lastStatus;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum CrawlType {
        STATIC,     // Jsoup으로 처리 가능
        DYNAMIC     // Selenium 필요
    }

    public enum CrawlStatus {
        SUCCESS,
        FAILED,
        PARTIAL
    }
}
