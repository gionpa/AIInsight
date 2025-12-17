package com.aiinsight.domain.article;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NewsArticleRepository extends JpaRepository<NewsArticle, Long> {

    // 전체 기사 최신순 조회
    Page<NewsArticle> findAllByOrderByCrawledAtDesc(Pageable pageable);

    boolean existsByContentHash(String contentHash);

    Optional<NewsArticle> findByContentHash(String contentHash);

    Page<NewsArticle> findByTargetIdOrderByCrawledAtDesc(Long targetId, Pageable pageable);

    Page<NewsArticle> findByIsNewTrueOrderByCrawledAtDesc(Pageable pageable);

    Page<NewsArticle> findByCategoryOrderByCrawledAtDesc(NewsArticle.ArticleCategory category, Pageable pageable);

    Page<NewsArticle> findByImportanceOrderByCrawledAtDesc(NewsArticle.ArticleImportance importance, Pageable pageable);

    /**
     * 중요도 및 발행일 범위로 기사 조회 (일일 리포트용)
     */
    List<NewsArticle> findByImportanceAndPublishedAtBetween(
            NewsArticle.ArticleImportance importance,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    /**
     * 중요도 및 크롤링일 범위로 기사 조회 (일일 리포트용 - crawledAt 기준)
     */
    List<NewsArticle> findByImportanceAndCrawledAtBetween(
            NewsArticle.ArticleImportance importance,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    /**
     * 중요도 및 크롤링일 범위로 기사 조회 (최신순 정렬)
     * Executive Summary 생성용
     */
    List<NewsArticle> findByImportanceAndCrawledAtBetweenOrderByCrawledAtDesc(
            NewsArticle.ArticleImportance importance,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    @Query("SELECT na FROM NewsArticle na WHERE na.isSummarized = false ORDER BY na.crawledAt ASC")
    List<NewsArticle> findUnsummarizedArticles(Pageable pageable);

    @Query("SELECT na FROM NewsArticle na WHERE na.relevanceScore >= :minScore ORDER BY na.crawledAt DESC")
    Page<NewsArticle> findByMinRelevanceScore(Double minScore, Pageable pageable);

    @Query("SELECT na FROM NewsArticle na WHERE " +
           "(LOWER(na.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(na.content) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY na.crawledAt DESC")
    Page<NewsArticle> searchByKeyword(String keyword, Pageable pageable);

    @Modifying
    @Query("UPDATE NewsArticle na SET na.isNew = false WHERE na.id IN :ids")
    void markAsRead(List<Long> ids);

    @Query("SELECT COUNT(na) FROM NewsArticle na WHERE na.isNew = true")
    Long countNewArticles();

    @Query("SELECT COUNT(na) FROM NewsArticle na WHERE na.crawledAt >= :startDate")
    Long countArticlesCrawledAfter(LocalDateTime startDate);

    @Query("SELECT na.category, COUNT(na) FROM NewsArticle na WHERE na.crawledAt >= :startDate GROUP BY na.category")
    List<Object[]> countByCategory(LocalDateTime startDate);

    Page<NewsArticle> findByCategoryAndImportanceOrderByRelevanceScoreDesc(
            NewsArticle.ArticleCategory category,
            NewsArticle.ArticleImportance importance,
            Pageable pageable);
}
