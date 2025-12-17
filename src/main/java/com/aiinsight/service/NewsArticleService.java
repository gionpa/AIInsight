package com.aiinsight.service;

import com.aiinsight.domain.article.NewsArticle;
import com.aiinsight.domain.article.NewsArticleRepository;
import com.aiinsight.domain.crawl.CrawlTarget;
import com.aiinsight.dto.NewsArticleDto;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class NewsArticleService {

    private final NewsArticleRepository newsArticleRepository;

    public Page<NewsArticleDto.Response> findAll(Pageable pageable) {
        return newsArticleRepository.findAllByOrderByCrawledAtDesc(pageable)
                .map(NewsArticleDto.Response::from);
    }

    public NewsArticleDto.DetailResponse findById(Long id) {
        NewsArticle article = newsArticleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("기사를 찾을 수 없습니다: " + id));
        return NewsArticleDto.DetailResponse.from(article);
    }

    /**
     * 엔티티 직접 조회 (내부 서비스 용도)
     */
    public NewsArticle findEntityById(Long id) {
        return newsArticleRepository.findById(id).orElse(null);
    }

    public Page<NewsArticleDto.Response> findByTargetId(Long targetId, Pageable pageable) {
        return newsArticleRepository.findByTargetIdOrderByCrawledAtDesc(targetId, pageable)
                .map(NewsArticleDto.Response::from);
    }

    public Page<NewsArticleDto.Response> findNewArticles(Pageable pageable) {
        return newsArticleRepository.findByIsNewTrueOrderByCrawledAtDesc(pageable)
                .map(NewsArticleDto.Response::from);
    }

    public Page<NewsArticleDto.Response> findByCategory(NewsArticle.ArticleCategory category, Pageable pageable) {
        return newsArticleRepository.findByCategoryOrderByCrawledAtDesc(category, pageable)
                .map(NewsArticleDto.Response::from);
    }

    public Page<NewsArticleDto.Response> search(String keyword, Pageable pageable) {
        return newsArticleRepository.searchByKeyword(keyword, pageable)
                .map(NewsArticleDto.Response::from);
    }

    public Page<NewsArticleDto.Response> findRelevantArticles(Double minScore, Pageable pageable) {
        return newsArticleRepository.findByMinRelevanceScore(minScore, pageable)
                .map(NewsArticleDto.Response::from);
    }

    public List<NewsArticle> findUnsummarizedArticles(int limit) {
        return newsArticleRepository.findUnsummarizedArticles(PageRequest.of(0, limit));
    }

    @Transactional
    public NewsArticle save(CrawlTarget target, String url, String title, String content,
                            String author, LocalDateTime publishedAt, String thumbnailUrl) {
        String contentHash = generateHash(url, title);

        // 중복 체크
        if (newsArticleRepository.existsByContentHash(contentHash)) {
            log.debug("이미 존재하는 기사: {}", title);
            return null;
        }

        NewsArticle article = NewsArticle.builder()
                .target(target)
                .originalUrl(url)
                .title(title)
                .content(content)
                .author(author)
                .publishedAt(publishedAt)
                .thumbnailUrl(thumbnailUrl)
                .contentHash(contentHash)
                .analysisStatus(NewsArticle.AnalysisStatus.PENDING)
                .isNew(true)
                .isSummarized(false)
                .build();

        NewsArticle saved = newsArticleRepository.save(article);
        log.info("새 기사 저장: {} ({})", saved.getTitle(), saved.getId());
        return saved;
    }

    @Transactional
    public void updateSummary(Long id, String titleKo, String summary, Double relevanceScore,
                              NewsArticle.ArticleCategory category, NewsArticle.ArticleImportance importance,
                              NewsArticle.UrgencyLevel urgencyLevel, NewsArticle.ImpactScope impactScope,
                              Double businessImpact, Double actionabilityScore, String mentionedCompanies) {
        NewsArticle article = newsArticleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("기사를 찾을 수 없습니다: " + id));

        if (titleKo != null && !titleKo.isEmpty()) {
            article.setTitleKo(titleKo);
        }
        article.setSummary(summary);
        article.setRelevanceScore(relevanceScore);
        article.setCategory(category);
        article.setImportance(importance);
        article.setUrgencyLevel(urgencyLevel);
        article.setImpactScope(impactScope);
        article.setBusinessImpact(businessImpact);
        article.setActionabilityScore(actionabilityScore);
        article.setMentionedCompanies(mentionedCompanies);
        article.setAnalysisStatus(NewsArticle.AnalysisStatus.COMPLETED);
        article.setIsSummarized(true);

        newsArticleRepository.save(article);
        log.info("기사 요약 완료: {} -> {} (점수: {}, 긴급도: {}, 영향: {})",
                article.getTitle(), titleKo, relevanceScore, urgencyLevel, businessImpact);
    }

    @Transactional
    public void updateTitleAndContent(Long id, String title, String content) {
        newsArticleRepository.findById(id).ifPresent(article -> {
            if (title != null && !title.isBlank()) {
                article.setTitle(title);
            }
            if (content != null && !content.isBlank()) {
                article.setContent(content);
            }
            newsArticleRepository.save(article);
            log.debug("기사 제목/본문 업데이트: {} -> {}", id, title);
        });
    }

    @Transactional
    public void markAsRead(List<Long> ids) {
        newsArticleRepository.markAsRead(ids);
        log.info("{}개 기사를 읽음 처리", ids.size());
    }

    @Transactional
    public void markAsRead(Long id) {
        NewsArticle article = newsArticleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("기사를 찾을 수 없습니다: " + id));
        article.setIsNew(false);
        newsArticleRepository.save(article);
    }

    public Long countNewArticles() {
        return newsArticleRepository.countNewArticles();
    }

    public Long countTodayArticles() {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        return newsArticleRepository.countArticlesCrawledAfter(startOfDay);
    }

    public boolean existsByHash(String url, String title) {
        String hash = generateHash(url, title);
        return newsArticleRepository.existsByContentHash(hash);
    }

    public Page<NewsArticleDto.Response> findByImportance(NewsArticle.ArticleImportance importance, Pageable pageable) {
        return newsArticleRepository.findByImportanceOrderByCrawledAtDesc(importance, pageable)
                .map(NewsArticleDto.Response::from);
    }

    @Transactional
    public void updateAnalysisStatus(Long id, NewsArticle.AnalysisStatus status) {
        newsArticleRepository.findById(id).ifPresent(article -> {
            article.setAnalysisStatus(status);
            newsArticleRepository.save(article);
        });
    }

    @Transactional
    public void delete(Long id) {
        if (newsArticleRepository.existsById(id)) {
            newsArticleRepository.deleteById(id);
            log.info("기사 삭제: ID {}", id);
        }
    }

    @Transactional
    public int deleteBatch(List<Long> ids) {
        int deleted = 0;
        for (Long id : ids) {
            if (newsArticleRepository.existsById(id)) {
                newsArticleRepository.deleteById(id);
                deleted++;
            }
        }
        log.info("{}개 기사 삭제 완료", deleted);
        return deleted;
    }

    /**
     * URL 정규화 - 쿼리 파라미터와 프래그먼트 제거하여 동일 기사 판별
     */
    private String normalizeUrl(String url) {
        if (url == null || url.isEmpty()) {
            return "";
        }
        // 쿼리 파라미터 제거
        int queryIndex = url.indexOf('?');
        if (queryIndex > 0) {
            url = url.substring(0, queryIndex);
        }
        // 프래그먼트 제거
        int fragmentIndex = url.indexOf('#');
        if (fragmentIndex > 0) {
            url = url.substring(0, fragmentIndex);
        }
        // 마지막 슬래시 제거 (일관성 유지)
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url.toLowerCase();
    }

    /**
     * URL 기반 해시 생성 - 같은 URL은 같은 기사로 판별
     * (제목이 약간 달라도 URL이 같으면 중복)
     */
    private String generateHash(String url, String title) {
        try {
            // URL만으로 해시 생성 (정규화된 URL 사용)
            String normalizedUrl = normalizeUrl(url);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(normalizedUrl.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 알고리즘을 찾을 수 없습니다", e);
        }
    }
}
