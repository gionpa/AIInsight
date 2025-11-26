package com.aiinsight.domain.crawl;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CrawlTargetRepository extends JpaRepository<CrawlTarget, Long> {

    List<CrawlTarget> findByEnabledTrue();

    List<CrawlTarget> findByEnabledTrueOrderByNameAsc();

    @Query("SELECT ct FROM CrawlTarget ct WHERE ct.enabled = true AND ct.cronExpression = :cronExpression")
    List<CrawlTarget> findByCronExpression(String cronExpression);

    boolean existsByUrl(String url);

    boolean existsByUrlAndIdNot(String url, Long id);
}
