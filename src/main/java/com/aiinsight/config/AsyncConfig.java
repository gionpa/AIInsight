package com.aiinsight.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig {

    /**
     * AI 분석용 비동기 Executor
     * - 크롤링 완료 후 AI 분석을 백그라운드에서 처리
     * - 크롤링 응답 시간에 영향을 주지 않음
     */
    @Bean(name = "aiAnalysisExecutor")
    public Executor aiAnalysisExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);      // 기본 스레드 수
        executor.setMaxPoolSize(5);       // 최대 스레드 수
        executor.setQueueCapacity(100);   // 대기 큐 크기
        executor.setThreadNamePrefix("ai-analysis-");
        executor.setRejectedExecutionHandler((r, e) -> {
            log.warn("AI 분석 작업 큐가 가득 찼습니다. 작업이 거부되었습니다.");
        });
        executor.initialize();
        return executor;
    }
}
