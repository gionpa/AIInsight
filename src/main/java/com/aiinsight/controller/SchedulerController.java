package com.aiinsight.controller;

import com.aiinsight.scheduler.CrawlScheduler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/scheduler")
@RequiredArgsConstructor
@Tag(name = "스케줄러 관리", description = "크롤링 스케줄러 관리 API")
public class SchedulerController {

    private final CrawlScheduler crawlScheduler;

    @PostMapping("/refresh")
    @Operation(summary = "스케줄 갱신", description = "모든 크롤링 스케줄을 다시 로드합니다")
    public ResponseEntity<SchedulerStatusResponse> refreshSchedules() {
        crawlScheduler.refreshSchedules();
        return ResponseEntity.ok(new SchedulerStatusResponse(
                "스케줄이 갱신되었습니다",
                crawlScheduler.getScheduledTaskCount()
        ));
    }

    @GetMapping("/status")
    @Operation(summary = "스케줄러 상태 조회", description = "현재 등록된 스케줄 수를 조회합니다")
    public ResponseEntity<SchedulerStatusResponse> getStatus() {
        return ResponseEntity.ok(new SchedulerStatusResponse(
                "정상 운영 중",
                crawlScheduler.getScheduledTaskCount()
        ));
    }

    @DeleteMapping("/cancel/{targetId}")
    @Operation(summary = "특정 타겟 스케줄 취소", description = "특정 크롤링 타겟의 스케줄을 취소합니다")
    public ResponseEntity<Void> cancelSchedule(@PathVariable Long targetId) {
        crawlScheduler.cancelSchedule(targetId);
        return ResponseEntity.ok().build();
    }

    public record SchedulerStatusResponse(String message, int scheduledTaskCount) {}
}
