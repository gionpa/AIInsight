package com.aiinsight.service;

import com.aiinsight.crawler.CrawlResult;
import com.aiinsight.crawler.SeleniumCrawler;
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

    private static final int MAX_RETRY_COUNT = 2;
    private static final long RETRY_DELAY_MS = 60000; // 1분

    private final WebCrawler webCrawler;
    private final SeleniumCrawler seleniumCrawler;
    private final CrawlTargetService crawlTargetService;
    private final NewsArticleService newsArticleService;
    private final CrawlHistoryService crawlHistoryService;
    private final AiSummaryService aiSummaryService;

    @Transactional
    public CrawlResult executeCrawl(Long targetId) {
        CrawlTarget target = crawlTargetService.findEntityById(targetId);
        return executeCrawlWithRetry(target);
    }

    @Transactional
    public CrawlResult executeCrawl(CrawlTarget target) {
        return executeCrawlWithRetry(target);
    }

    private CrawlResult executeCrawlWithRetry(CrawlTarget target) {
        CrawlResult result = doExecuteCrawl(target);

        // 실패 시 재시도
        int retryCount = 0;
        while (!result.isSuccess() && retryCount < MAX_RETRY_COUNT) {
            retryCount++;
            log.info("크롤링 재시도 예정: {} ({}번째, 1분 후)", target.getName(), retryCount);

            try {
                Thread.sleep(RETRY_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("재시도 대기 중 인터럽트 발생");
                break;
            }

            log.info("크롤링 재시도 시작: {} ({}번째)", target.getName(), retryCount);
            result = doExecuteCrawl(target);
        }

        return result;
    }

    private CrawlResult doExecuteCrawl(CrawlTarget target) {
        log.info("크롤링 시작: {} ({}) - 타입: {}", target.getName(), target.getUrl(), target.getCrawlType());

        // crawlType에 따라 적절한 크롤러 선택
        CrawlResult result;
        if (target.getCrawlType() == CrawlTarget.CrawlType.DYNAMIC) {
            log.info("DYNAMIC 크롤링 사용 (Selenium): {}", target.getName());
            result = seleniumCrawler.crawl(target);
        } else {
            log.info("STATIC 크롤링 사용 (Jsoup): {}", target.getName());
            result = webCrawler.crawl(target);
        }

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

                        // 크롤링 시점에 AI 분석 수행 (한글 제목, 요약, 카테고리, 중요도)
                        try {
                            log.info("AI 분석 시작: {} (ID: {})", saved.getTitle(), saved.getId());
                            aiSummaryService.summarizeArticle(saved);
                            log.info("AI 분석 완료: {} (ID: {})", saved.getTitle(), saved.getId());
                        } catch (Exception e) {
                            log.warn("AI 분석 실패 (나중에 재시도 가능): {} - {}", saved.getId(), e.getMessage());
                        }
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
                executeCrawlWithRetry(target);

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
