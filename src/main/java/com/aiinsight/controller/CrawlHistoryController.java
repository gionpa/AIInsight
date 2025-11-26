package com.aiinsight.controller;

import com.aiinsight.dto.CrawlHistoryDto;
import com.aiinsight.service.CrawlHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/crawl-history")
@RequiredArgsConstructor
@Tag(name = "크롤링 이력 관리", description = "크롤링 실행 이력 조회 API")
public class CrawlHistoryController {

    private final CrawlHistoryService crawlHistoryService;

    @GetMapping("/target/{targetId}")
    @Operation(summary = "타겟별 크롤링 이력 조회", description = "특정 크롤링 타겟의 실행 이력을 조회합니다")
    public ResponseEntity<Page<CrawlHistoryDto.Response>> findByTargetId(
            @PathVariable Long targetId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(crawlHistoryService.findByTargetId(targetId, pageable));
    }

    @GetMapping("/target/{targetId}/latest")
    @Operation(summary = "타겟의 최근 크롤링 이력 조회", description = "특정 크롤링 타겟의 가장 최근 실행 이력을 조회합니다")
    public ResponseEntity<CrawlHistoryDto.Response> findLatestByTargetId(@PathVariable Long targetId) {
        return crawlHistoryService.findLatestByTargetId(targetId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/today")
    @Operation(summary = "오늘의 크롤링 이력 조회", description = "오늘 실행된 모든 크롤링 이력을 조회합니다")
    public ResponseEntity<List<CrawlHistoryDto.Response>> findTodayHistory() {
        return ResponseEntity.ok(crawlHistoryService.findTodayHistory());
    }

    @GetMapping("/stats")
    @Operation(summary = "오늘의 크롤링 통계", description = "오늘의 성공/실패 크롤링 횟수를 조회합니다")
    public ResponseEntity<CrawlStatsResponse> getTodayStats() {
        return ResponseEntity.ok(new CrawlStatsResponse(
                crawlHistoryService.countSuccessfulToday(),
                crawlHistoryService.countFailedToday()
        ));
    }

    public record CrawlStatsResponse(Long successCount, Long failCount) {}
}
