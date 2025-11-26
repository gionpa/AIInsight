package com.aiinsight.controller;

import com.aiinsight.dto.DashboardDto;
import com.aiinsight.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "대시보드", description = "대시보드 통계 API")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    @Operation(summary = "대시보드 통계 조회", description = "전체 통계 정보를 조회합니다")
    public ResponseEntity<DashboardDto.Stats> getStats() {
        return ResponseEntity.ok(dashboardService.getStats());
    }
}
