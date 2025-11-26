package com.aiinsight.service;

import com.aiinsight.domain.article.NewsArticle;
import com.aiinsight.domain.article.NewsArticleRepository;
import com.aiinsight.domain.crawl.CrawlHistoryRepository;
import com.aiinsight.domain.crawl.CrawlTarget;
import com.aiinsight.domain.crawl.CrawlTargetRepository;
import com.aiinsight.dto.DashboardDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DashboardService {

    private final CrawlTargetRepository crawlTargetRepository;
    private final CrawlHistoryRepository crawlHistoryRepository;
    private final NewsArticleRepository newsArticleRepository;

    public DashboardDto.Stats getStats() {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime last7Days = LocalDateTime.now().minusDays(7);

        // 타겟 통계
        long totalTargets = crawlTargetRepository.count();
        long activeTargets = crawlTargetRepository.findByEnabledTrue().size();

        // 기사 통계
        long totalArticles = newsArticleRepository.count();
        long newArticles = newsArticleRepository.countNewArticles();
        long todayCrawled = newsArticleRepository.countArticlesCrawledAfter(startOfDay);

        // 크롤링 통계
        long successfulCrawls = crawlHistoryRepository.countByStatusAfter(CrawlTarget.CrawlStatus.SUCCESS, startOfDay);
        long failedCrawls = crawlHistoryRepository.countByStatusAfter(CrawlTarget.CrawlStatus.FAILED, startOfDay);

        // 카테고리 분포
        Map<NewsArticle.ArticleCategory, Long> categoryDistribution = new HashMap<>();
        List<Object[]> categoryCounts = newsArticleRepository.countByCategory(last7Days);
        for (Object[] row : categoryCounts) {
            if (row[0] != null) {
                categoryDistribution.put((NewsArticle.ArticleCategory) row[0], (Long) row[1]);
            }
        }

        // 최근 크롤링 기록 (최근 24시간)
        LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
        List<DashboardDto.RecentCrawl> recentCrawls = crawlHistoryRepository.findByExecutedAtAfterOrderByExecutedAtDesc(last24Hours)
                .stream()
                .limit(10)
                .map(h -> DashboardDto.RecentCrawl.builder()
                        .targetId(h.getTarget().getId())
                        .targetName(h.getTarget().getName())
                        .status(h.getStatus().name())
                        .articlesNew(h.getArticlesNew())
                        .executedAt(h.getExecutedAt().format(DateTimeFormatter.ofPattern("MM-dd HH:mm:ss")))
                        .build())
                .collect(Collectors.toList());

        return DashboardDto.Stats.builder()
                .totalTargets(totalTargets)
                .activeTargets(activeTargets)
                .totalArticles(totalArticles)
                .newArticles(newArticles)
                .todayCrawled(todayCrawled)
                .successfulCrawls(successfulCrawls)
                .failedCrawls(failedCrawls)
                .categoryDistribution(categoryDistribution)
                .recentCrawls(recentCrawls)
                .build();
    }
}
