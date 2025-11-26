package com.aiinsight.domain.crawl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CrawlHistoryRepository extends JpaRepository<CrawlHistory, Long> {

    Page<CrawlHistory> findByTargetIdOrderByExecutedAtDesc(Long targetId, Pageable pageable);

    Optional<CrawlHistory> findTopByTargetIdOrderByExecutedAtDesc(Long targetId);

    List<CrawlHistory> findByExecutedAtAfter(LocalDateTime dateTime);

    @Query("SELECT ch FROM CrawlHistory ch WHERE ch.target.id = :targetId AND ch.executedAt >= :startDate ORDER BY ch.executedAt DESC")
    List<CrawlHistory> findRecentByTargetId(Long targetId, LocalDateTime startDate);

    @Query("SELECT COUNT(ch) FROM CrawlHistory ch WHERE ch.status = :status AND ch.executedAt >= :startDate")
    Long countByStatusAfter(CrawlTarget.CrawlStatus status, LocalDateTime startDate);
}
