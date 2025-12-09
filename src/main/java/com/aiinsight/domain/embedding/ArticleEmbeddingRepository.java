package com.aiinsight.domain.embedding;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ArticleEmbeddingRepository extends JpaRepository<ArticleEmbedding, Long> {

    /**
     * 기사 ID로 임베딩 조회
     */
    Optional<ArticleEmbedding> findByArticleId(Long articleId);

    /**
     * 임베딩이 없는 기사 ID 목록 조회
     */
    @Query("SELECT a.id FROM NewsArticle a WHERE a.id NOT IN (SELECT ae.article.id FROM ArticleEmbedding ae)")
    List<Long> findArticleIdsWithoutEmbedding();

    /**
     * 특정 기간 내 생성된 임베딩 조회
     */
    List<ArticleEmbedding> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 코사인 유사도 기반 유사 기사 검색
     * - pgvector의 <=> 연산자 사용 (코사인 거리)
     * - 거리가 가까울수록 유사함
     */
    @Query(value = """
        SELECT ae.*, 1 - (ae.embedding_vector <=> CAST(:queryVector AS vector)) AS similarity
        FROM article_embedding ae
        WHERE ae.article_id != :excludeArticleId
        ORDER BY ae.embedding_vector <=> CAST(:queryVector AS vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findSimilarArticles(
            @Param("queryVector") String queryVector,
            @Param("excludeArticleId") Long excludeArticleId,
            @Param("limit") int limit
    );

    /**
     * 특정 기간 내 기사들의 임베딩으로 유사 기사 검색
     */
    @Query(value = """
        SELECT ae.*, 1 - (ae.embedding_vector <=> CAST(:queryVector AS vector)) AS similarity
        FROM article_embedding ae
        INNER JOIN news_article na ON ae.article_id = na.id
        WHERE na.crawled_at BETWEEN :startDate AND :endDate
          AND ae.article_id != :excludeArticleId
        ORDER BY ae.embedding_vector <=> CAST(:queryVector AS vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findSimilarArticlesInPeriod(
            @Param("queryVector") String queryVector,
            @Param("excludeArticleId") Long excludeArticleId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("limit") int limit
    );

    /**
     * 품질 점수 기준 이상의 임베딩 조회
     */
    List<ArticleEmbedding> findByQualityScoreGreaterThanEqual(Double minQualityScore);

    /**
     * 특정 모델로 생성된 임베딩 개수 조회
     */
    long countByModelName(String modelName);

    /**
     * 기사 ID 존재 여부 확인
     */
    boolean existsByArticleId(Long articleId);
}
