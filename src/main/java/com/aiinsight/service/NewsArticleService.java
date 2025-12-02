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
                .isNew(true)
                .isSummarized(false)
                .build();

        NewsArticle saved = newsArticleRepository.save(article);
        log.info("새 기사 저장: {} ({})", saved.getTitle(), saved.getId());
        return saved;
    }

    @Transactional
    public void updateSummary(Long id, String titleKo, String summary, Double relevanceScore,
                              NewsArticle.ArticleCategory category, NewsArticle.ArticleImportance importance) {
        NewsArticle article = newsArticleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("기사를 찾을 수 없습니다: " + id));

        if (titleKo != null && !titleKo.isEmpty()) {
            article.setTitleKo(titleKo);
        }
        article.setSummary(summary);
        article.setRelevanceScore(relevanceScore);
        article.setCategory(category);
        article.setImportance(importance);
        article.setIsSummarized(true);

        newsArticleRepository.save(article);
        log.info("기사 요약 완료: {} -> {} (점수: {})", article.getTitle(), titleKo, relevanceScore);
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

    public NewsArticle findEntityById(Long id) {
        return newsArticleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("기사를 찾을 수 없습니다: " + id));
    }

    public Page<NewsArticleDto.Response> findByImportance(NewsArticle.ArticleImportance importance, Pageable pageable) {
        return newsArticleRepository.findByImportanceOrderByCrawledAtDesc(importance, pageable)
                .map(NewsArticleDto.Response::from);
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

    private String generateHash(String url, String title) {
        try {
            String input = url + "|" + title;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 알고리즘을 찾을 수 없습니다", e);
        }
    }
}
