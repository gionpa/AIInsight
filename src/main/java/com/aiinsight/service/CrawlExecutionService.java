package com.aiinsight.service;

import com.aiinsight.crawler.CrawlResult;
import com.aiinsight.crawler.WebCrawler;
import com.aiinsight.domain.article.NewsArticle;
import com.aiinsight.domain.crawl.CrawlTarget;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CrawlExecutionService {

    private final WebCrawler webCrawler;
    private final CrawlTargetService crawlTargetService;
    private final NewsArticleService newsArticleService;
    private final CrawlHistoryService crawlHistoryService;

    @Transactional
    public CrawlResult executeCrawl(Long targetId) {
        CrawlTarget target = crawlTargetService.findEntityById(targetId);
        return executeCrawl(target);
    }

    @Transactional
    public CrawlResult executeCrawl(CrawlTarget target) {
        log.info("크롤링 시작: {} ({})", target.getName(), target.getUrl());

        CrawlResult result = webCrawler.crawl(target);

        if (result.isSuccess()) {
            int newArticles = 0;

            for (CrawlResult.ArticleData articleData : result.getArticles()) {
                // 중복 체크 후 저장
                if (!newsArticleService.existsByHash(articleData.getUrl(), articleData.getTitle())) {
                    NewsArticle saved = newsArticleService.save(
                            target,
                            articleData.getUrl(),
                            articleData.getTitle(),
                            articleData.getContent(),
                            articleData.getAuthor(),
                            articleData.getPublishedAt(),
                            articleData.getThumbnailUrl()
                    );
                    if (saved != null) {
                        newArticles++;
                    }
                }
            }

            // 크롤링 이력 기록
            crawlHistoryService.recordSuccess(target, result.getArticleCount(), newArticles, result.getDurationMs());

            // 타겟 상태 업데이트
            crawlTargetService.updateLastCrawlStatus(target.getId(), CrawlTarget.CrawlStatus.SUCCESS);

            log.info("크롤링 완료: {} - 총 {}개 기사 발견, {}개 신규 저장",
                    target.getName(), result.getArticleCount(), newArticles);
        } else {
            // 실패 이력 기록
            crawlHistoryService.recordFailure(target, result.getErrorMessage(), result.getDurationMs());
            crawlTargetService.updateLastCrawlStatus(target.getId(), CrawlTarget.CrawlStatus.FAILED);

            log.error("크롤링 실패: {} - {}", target.getName(), result.getErrorMessage());
        }

        return result;
    }

    public void executeAllEnabledCrawls() {
        log.info("전체 크롤링 시작");

        for (CrawlTarget target : crawlTargetService.findEnabledTargets()) {
            try {
                executeCrawl(target);

                // 요청 간 딜레이
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("크롤링이 중단되었습니다");
                break;
            } catch (Exception e) {
                log.error("크롤링 중 오류 발생: {} - {}", target.getName(), e.getMessage());
            }
        }

        log.info("전체 크롤링 완료");
    }
}
