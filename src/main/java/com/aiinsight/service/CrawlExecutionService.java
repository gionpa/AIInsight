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
            // Selenium 사용 가능 여부 확인
            if (seleniumCrawler.isAvailable()) {
                log.info("DYNAMIC 크롤링 사용 (Selenium): {}", target.getName());
                result = seleniumCrawler.crawl(target);

                // Selenium 실패 시 Jsoup으로 폴백 시도
                if (!result.isSuccess() && result.getErrorMessage() != null &&
                        (result.getErrorMessage().contains("Could not start a new session") ||
                         result.getErrorMessage().contains("session not created") ||
                         result.getErrorMessage().contains("unable to connect"))) {
                    log.warn("Selenium 세션 생성 실패, Jsoup으로 폴백 시도: {}", target.getName());
                    result = webCrawler.crawl(target);
                    if (result.isSuccess()) {
                        log.info("Jsoup 폴백 크롤링 성공: {} - {}개 기사", target.getName(), result.getArticleCount());
                    }
                }
            } else {
                // Selenium 사용 불가 시 Jsoup으로 폴백
                log.warn("Selenium 사용 불가 ({}), Jsoup으로 폴백: {}",
                        seleniumCrawler.getUnavailableReason(), target.getName());
                result = webCrawler.crawl(target);
            }
        } else {
            log.info("STATIC 크롤링 사용 (Jsoup): {}", target.getName());
            result = webCrawler.crawl(target);
        }

        if (result.isSuccess()) {
            int newArticles = 0;

            for (CrawlResult.ArticleData articleData : result.getArticles()) {
                // 빈 제목 또는 URL 체크
                String title = articleData.getTitle();
                String url = articleData.getUrl();
                if (url == null || url.isEmpty()) {
                    log.warn("URL이 없는 기사 건너뜀");
                    continue;
                }

                // 이미지/미디어 URL 필터링
                if (isMediaUrl(url)) {
                    log.warn("미디어 파일 URL 건너뜀: {}", url);
                    continue;
                }

                // 중복 체크 후 저장
                if (!newsArticleService.existsByHash(url, title)) {
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

                        // 비동기로 AI 분석 수행 (크롤링 응답을 블로킹하지 않음)
                        log.info("AI 분석 비동기 요청: {} (ID: {})", saved.getTitle(), saved.getId());
                        aiSummaryService.summarizeArticleAsync(saved.getId());
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

    /**
     * URL이 이미지/미디어 파일인지 확인
     */
    private boolean isMediaUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        String lowerUrl = url.toLowerCase();
        // 이미지 확장자
        String[] mediaExtensions = {".png", ".jpg", ".jpeg", ".gif", ".webp", ".svg", ".ico",
                                    ".bmp", ".tiff", ".mp4", ".mp3", ".wav", ".avi", ".mov", ".pdf"};
        for (String ext : mediaExtensions) {
            if (lowerUrl.endsWith(ext) || lowerUrl.contains(ext + "?")) {
                return true;
            }
        }
        return false;
    }
}
