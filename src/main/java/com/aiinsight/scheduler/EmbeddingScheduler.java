package com.aiinsight.scheduler;

import com.aiinsight.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 임베딩 생성 스케줄러
 * - 임베딩이 없는 기사에 대해 주기적으로 임베딩 생성
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmbeddingScheduler {

    private final EmbeddingService embeddingService;

    /**
     * 매 30분마다 임베딩이 없는 기사에 대해 임베딩 생성
     * - 한 번에 최대 50개 처리
     */
    @Scheduled(cron = "0 */30 * * * *") // 매 30분마다
    public void generateEmbeddingsForNewArticles() {
        log.info("임베딩 생성 스케줄러 시작");

        try {
            int generatedCount = embeddingService.generateEmbeddingsForArticlesWithoutEmbedding(50);
            log.info("임베딩 생성 스케줄러 완료: {}개 생성", generatedCount);

        } catch (Exception e) {
            log.error("임베딩 생성 스케줄러 실패: {}", e.getMessage(), e);
        }
    }
}
