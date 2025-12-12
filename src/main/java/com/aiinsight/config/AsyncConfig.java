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
        executor.setCorePoolSize(1);      // 기본 스레드 수 (순차 처리)
        executor.setMaxPoolSize(1);       // 최대 스레드 수 (순차 처리 보장)
        executor.setQueueCapacity(100);   // 대기 큐 크기
        executor.setThreadNamePrefix("ai-analysis-");

        // 우아한 종료 설정 - 리소스 누수 방지
        executor.setWaitForTasksToCompleteOnShutdown(true);  // 종료 시 실행 중인 작업 완료 대기
        executor.setAwaitTerminationSeconds(30);              // 최대 30초 대기
        executor.setAllowCoreThreadTimeOut(true);             // 유휴 스레드 타임아웃 허용
        executor.setKeepAliveSeconds(60);                     // 유휴 스레드 60초 후 종료

        executor.setRejectedExecutionHandler((r, e) -> {
            log.warn("AI 분석 작업 큐가 가득 찼습니다. 작업이 거부되었습니다.");
        });
        executor.initialize();
        return executor;
    }
}
