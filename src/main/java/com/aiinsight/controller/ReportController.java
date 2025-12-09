package com.aiinsight.controller;

import com.aiinsight.domain.report.DailyReport;
import com.aiinsight.dto.DailyReportResponse;
import com.aiinsight.dto.ReportDto;
import com.aiinsight.service.DailyReportService;
import com.aiinsight.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "리포트", description = "AI 뉴스 분석 리포트 API")
public class ReportController {

    private final ReportService reportService;
    private final DailyReportService dailyReportService;

    /**
     * Phase 3: 최신 일일 리포트 조회 (레거시 형식)
     * GET /api/reports/latest
     */
    @GetMapping("/latest")
    @Operation(summary = "최신 리포트 조회", description = "가장 최근에 생성된 일일 리포트를 레거시 형식으로 반환합니다")
    public ResponseEntity<ReportDto.DailyReport> getLatestReport() {
        // 레거시 ReportService 사용
        return ResponseEntity.ok(reportService.generateDailyReport());
    }

    /**
     * Phase 3: 최신 일일 리포트 조회 (Phase 3 형식 - 임베딩 기반 토픽 클러스터링)
     * GET /api/reports/latest-phase3
     */
    @GetMapping("/latest-phase3")
    @Operation(summary = "최신 리포트 조회 (Phase 3)", description = "임베딩 기반 토픽 클러스터링을 사용한 최신 일일 리포트를 반환합니다")
    public ResponseEntity<DailyReportResponse> getLatestPhase3Report() {
        Optional<DailyReport> report = dailyReportService.getLatestReport();

        if (report.isPresent()) {
            DailyReportResponse response = DailyReportResponse.fromEntity(report.get());
            return ResponseEntity.ok(response);
        } else {
            // 리포트가 없으면 오늘 리포트를 자동 생성
            try {
                LocalDate today = LocalDate.now();
                DailyReport newReport = dailyReportService.generateDailyReport(today);

                if (newReport != null) {
                    DailyReportResponse response = DailyReportResponse.fromEntity(newReport);
                    return ResponseEntity.ok(response);
                } else {
                    // 생성 실패 시 빈 응답 반환
                    return ResponseEntity.ok(createEmptyReport());
                }
            } catch (Exception e) {
                // 예외 발생 시 빈 응답 반환
                return ResponseEntity.ok(createEmptyReport());
            }
        }
    }

    /**
     * 빈 리포트 응답 생성
     */
    private DailyReportResponse createEmptyReport() {
        return DailyReportResponse.builder()
                .id(0L)
                .reportDate(LocalDate.now())
                .executiveSummary("아직 생성된 리포트가 없습니다. 기사가 수집되면 자동으로 리포트가 생성됩니다.")
                .keyTrends(List.of())
                .topicSummaries(List.of())
                .totalArticles(0)
                .highImportanceArticles(0)
                .status("PENDING")
                .generatedAt(LocalDate.now().atStartOfDay())
                .build();
    }

    /**
     * Phase 3: 특정 날짜 리포트 조회
     * GET /api/reports/daily?date=2025-12-09
     */
    @GetMapping("/daily")
    @Operation(summary = "특정 날짜 리포트 조회", description = "특정 날짜의 일일 리포트를 조회합니다")
    public ResponseEntity<DailyReportResponse> getDailyReportByDate(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date
    ) {
        // 날짜가 지정되지 않으면 레거시 동작 (ReportService 사용)
        if (date == null) {
            return ResponseEntity.ok(
                DailyReportResponse.builder()
                    .executiveSummary("레거시 리포트")
                    .build()
            );
        }

        Optional<DailyReport> report = dailyReportService.getReportByDate(date);

        if (report.isPresent()) {
            DailyReportResponse response = DailyReportResponse.fromEntity(report.get());
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Phase 3: 리포트 수동 생성
     * POST /api/reports/generate?date=2025-12-09
     */
    @PostMapping("/generate")
    @Operation(summary = "리포트 수동 생성", description = "특정 날짜의 리포트를 수동으로 생성합니다")
    public ResponseEntity<DailyReportResponse> generateReport(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date
    ) {
        // 날짜가 지정되지 않으면 어제 날짜 사용
        LocalDate targetDate = date != null ? date : LocalDate.now().minusDays(1);

        try {
            DailyReport report = dailyReportService.generateDailyReport(targetDate);

            if (report != null) {
                DailyReportResponse response = DailyReportResponse.fromEntity(report);
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Phase 3: 오늘 리포트 생성 (테스트용)
     * POST /api/reports/generate/today
     */
    @PostMapping("/generate/today")
    @Operation(summary = "오늘 리포트 생성", description = "오늘 날짜의 리포트를 생성합니다 (테스트용)")
    public ResponseEntity<DailyReportResponse> generateTodayReport() {
        LocalDate today = LocalDate.now();

        try {
            DailyReport report = dailyReportService.generateDailyReport(today);

            if (report != null) {
                DailyReportResponse response = DailyReportResponse.fromEntity(report);
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Phase 3: 리포트 존재 여부 확인
     * GET /api/reports/exists?date=2025-12-09
     */
    @GetMapping("/exists")
    @Operation(summary = "리포트 존재 여부 확인", description = "특정 날짜의 리포트가 존재하는지 확인합니다")
    public ResponseEntity<Boolean> checkReportExists(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date
    ) {
        Optional<DailyReport> report = dailyReportService.getReportByDate(date);
        return ResponseEntity.ok(report.isPresent());
    }

    /**
     * Phase 3: 헬스 체크
     * GET /api/reports/health
     */
    @GetMapping("/health")
    @Operation(summary = "헬스 체크", description = "Report API 상태를 확인합니다")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Daily Report API is running");
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
