package com.aiinsight.scheduler;

import com.aiinsight.domain.report.DailyReport;
import com.aiinsight.service.DailyReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 일일 리포트 생성 스케줄러
 * - 매일 자정에 전날의 리포트 생성
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DailyReportScheduler {

    private final DailyReportService dailyReportService;

    /**
     * 매일 오전 1시에 전날의 일일 리포트 생성
     * - 자정에 크롤링이 완료되고 임베딩 생성이 어느 정도 진행된 후 실행
     */
    @Scheduled(cron = "0 0 1 * * *")  // 매일 오전 1시
    public void generateYesterdayReport() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("일일 리포트 생성 스케줄러 시작: {}", yesterday);

        try {
            DailyReport report = dailyReportService.generateDailyReport(yesterday);

            if (report != null) {
                log.info("일일 리포트 생성 완료: {} (ID: {}, 기사 수: {})",
                        yesterday, report.getId(), report.getTotalArticles());
            } else {
                log.warn("일일 리포트 생성 실패: {} (기사 또는 임베딩 부족)", yesterday);
            }

        } catch (Exception e) {
            log.error("일일 리포트 생성 스케줄러 실패: {}", yesterday, e);
        }
    }

    /**
     * 오늘 리포트 생성 (테스트용)
     * - 수동으로 오늘의 리포트를 생성할 때 사용
     */
    public DailyReport generateTodayReportManually() {
        LocalDate today = LocalDate.now();
        log.info("오늘 리포트 수동 생성: {}", today);

        try {
            return dailyReportService.generateDailyReport(today);
        } catch (Exception e) {
            log.error("오늘 리포트 수동 생성 실패", e);
            return null;
        }
    }
}
