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

    List<CrawlHistory> findByExecutedAtAfterOrderByExecutedAtDesc(LocalDateTime dateTime);

    // 전체 이력 페이징 조회
    Page<CrawlHistory> findAllByOrderByExecutedAtDesc(Pageable pageable);

    @Query("SELECT ch FROM CrawlHistory ch WHERE ch.target.id = :targetId AND ch.executedAt >= :startDate ORDER BY ch.executedAt DESC")
    List<CrawlHistory> findRecentByTargetId(Long targetId, LocalDateTime startDate);

    @Query("SELECT COUNT(ch) FROM CrawlHistory ch WHERE ch.status = :status AND ch.executedAt >= :startDate")
    Long countByStatusAfter(CrawlTarget.CrawlStatus status, LocalDateTime startDate);

    // 한달 이전 이력 삭제
    void deleteByExecutedAtBefore(LocalDateTime dateTime);

    // 한달 이전 이력 개수 조회
    long countByExecutedAtBefore(LocalDateTime dateTime);
}
