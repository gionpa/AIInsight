package com.aiinsight.scheduler;

import com.aiinsight.domain.crawl.CrawlTarget;
import com.aiinsight.service.CrawlExecutionService;
import com.aiinsight.service.CrawlTargetService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class CrawlScheduler {

    private final TaskScheduler taskScheduler;
    private final CrawlTargetService crawlTargetService;
    private final CrawlExecutionService crawlExecutionService;

    // 등록된 스케줄 작업 관리
    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("크롤링 스케줄러 초기화 시작");
        refreshSchedules();
        log.info("크롤링 스케줄러 초기화 완료");
    }

    /**
     * 모든 활성화된 타겟의 스케줄을 갱신합니다.
     */
    public void refreshSchedules() {
        // 기존 스케줄 모두 취소
        cancelAllSchedules();

        // 활성화된 타겟들의 스케줄 등록
        for (CrawlTarget target : crawlTargetService.findEnabledTargets()) {
            scheduleTarget(target);
        }

        log.info("스케줄 갱신 완료: {} 개 타겟 등록", scheduledTasks.size());
    }

    /**
     * 특정 타겟의 스케줄을 등록합니다.
     */
    public void scheduleTarget(CrawlTarget target) {
        if (!target.getEnabled()) {
            log.debug("비활성화된 타겟 스킵: {}", target.getName());
            return;
        }

        try {
            CronTrigger trigger = new CronTrigger(target.getCronExpression());

            ScheduledFuture<?> future = taskScheduler.schedule(
                    () -> executeCrawlTask(target.getId()),
                    trigger
            );

            scheduledTasks.put(target.getId(), future);
            log.info("스케줄 등록: {} (cron: {})", target.getName(), target.getCronExpression());

        } catch (IllegalArgumentException e) {
            log.error("잘못된 Cron 표현식: {} - {}", target.getName(), target.getCronExpression());
        }
    }

    /**
     * 특정 타겟의 스케줄을 취소합니다.
     */
    public void cancelSchedule(Long targetId) {
        ScheduledFuture<?> future = scheduledTasks.remove(targetId);
        if (future != null) {
            future.cancel(false);
            log.info("스케줄 취소: targetId={}", targetId);
        }
    }

    /**
     * 모든 스케줄을 취소합니다.
     */
    public void cancelAllSchedules() {
        for (Map.Entry<Long, ScheduledFuture<?>> entry : scheduledTasks.entrySet()) {
            entry.getValue().cancel(false);
        }
        scheduledTasks.clear();
        log.info("모든 스케줄 취소됨");
    }

    /**
     * 특정 타겟의 스케줄을 업데이트합니다.
     */
    public void updateSchedule(CrawlTarget target) {
        cancelSchedule(target.getId());
        if (target.getEnabled()) {
            scheduleTarget(target);
        }
    }

    /**
     * 현재 등록된 스케줄 수를 반환합니다.
     */
    public int getScheduledTaskCount() {
        return scheduledTasks.size();
    }

    /**
     * 크롤링 작업을 실행합니다.
     */
    private void executeCrawlTask(Long targetId) {
        try {
            log.info("스케줄된 크롤링 시작: targetId={}", targetId);
            crawlExecutionService.executeCrawl(targetId);
        } catch (Exception e) {
            log.error("스케줄된 크롤링 실패: targetId={}, error={}", targetId, e.getMessage());
        }
    }
}
