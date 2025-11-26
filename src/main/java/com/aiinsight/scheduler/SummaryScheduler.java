package com.aiinsight.scheduler;

import com.aiinsight.domain.article.NewsArticle;
import com.aiinsight.service.AiSummaryService;
import com.aiinsight.service.NewsArticleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SummaryScheduler {

    private final NewsArticleService newsArticleService;
    private final AiSummaryService aiSummaryService;

    /**
     * 5분마다 요약되지 않은 기사를 처리합니다.
     */
    @Scheduled(fixedDelay = 300000, initialDelay = 60000) // 5분마다, 시작 후 1분 후 첫 실행
    public void processPendingSummaries() {
        log.debug("요약 대기 기사 처리 시작");

        List<NewsArticle> unsummarizedArticles = newsArticleService.findUnsummarizedArticles(10);

        if (unsummarizedArticles.isEmpty()) {
            log.debug("요약할 기사가 없습니다");
            return;
        }

        log.info("{}개 기사 요약 처리 시작", unsummarizedArticles.size());

        for (NewsArticle article : unsummarizedArticles) {
            try {
                aiSummaryService.summarizeArticle(article);

                // API 호출 간격 조절
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("기사 요약 실패: {} - {}", article.getId(), e.getMessage());
            }
        }

        log.info("요약 처리 완료");
    }
}
