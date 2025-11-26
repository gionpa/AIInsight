package com.aiinsight.controller;

import com.aiinsight.dto.ReportDto;
import com.aiinsight.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "리포트", description = "AI 뉴스 분석 리포트 API")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/daily")
    @Operation(summary = "일일 리포트 조회", description = "HIGH 중요도 기사 기반의 일일 리포트를 생성합니다")
    public ResponseEntity<ReportDto.DailyReport> getDailyReport() {
        return ResponseEntity.ok(reportService.generateDailyReport());
    }

    @GetMapping("/by-category")
    @Operation(summary = "카테고리별 리포트 조회", description = "카테고리별로 그룹화된 HIGH 중요도 기사 리포트를 생성합니다")
    public ResponseEntity<List<ReportDto.CategoryReport>> getCategoryReport() {
        return ResponseEntity.ok(reportService.generateCategoryReport());
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "특정 카테고리 리포트 조회", description = "특정 카테고리의 HIGH 중요도 기사 리포트를 조회합니다")
    public ResponseEntity<ReportDto.CategoryReport> getReportByCategory(@PathVariable String category) {
        return ResponseEntity.ok(reportService.getReportByCategory(category));
    }
}
