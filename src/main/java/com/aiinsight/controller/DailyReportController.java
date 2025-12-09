package com.aiinsight.controller;

import com.aiinsight.domain.report.DailyReport;
import com.aiinsight.dto.DailyReportResponse;
import com.aiinsight.service.DailyReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Optional;

/**
 * 일일 리포트 API 컨트롤러
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class DailyReportController {

    private final DailyReportService dailyReportService;

    /**
     * 최신 리포트 조회
     * GET /api/reports/latest
     */
    @GetMapping("/latest")
    public ResponseEntity<DailyReportResponse> getLatestReport() {
        log.info("최신 리포트 조회 요청");

        Optional<DailyReport> report = dailyReportService.getLatestReport();

        if (report.isPresent()) {
            DailyReportResponse response = DailyReportResponse.fromEntity(report.get());
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 특정 날짜 리포트 조회
     * GET /api/reports/daily?date=2025-12-09
     */
    @GetMapping("/daily")
    public ResponseEntity<DailyReportResponse> getDailyReport(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date
    ) {
        log.info("일일 리포트 조회 요청: {}", date);

        Optional<DailyReport> report = dailyReportService.getReportByDate(date);

        if (report.isPresent()) {
            DailyReportResponse response = DailyReportResponse.fromEntity(report.get());
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 리포트 수동 생성
     * POST /api/reports/generate?date=2025-12-09
     */
    @PostMapping("/generate")
    public ResponseEntity<DailyReportResponse> generateReport(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date
    ) {
        // 날짜가 지정되지 않으면 어제 날짜 사용
        LocalDate targetDate = date != null ? date : LocalDate.now().minusDays(1);
        log.info("리포트 수동 생성 요청: {}", targetDate);

        try {
            DailyReport report = dailyReportService.generateDailyReport(targetDate);

            if (report != null) {
                DailyReportResponse response = DailyReportResponse.fromEntity(report);
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(null);
            }
        } catch (Exception e) {
            log.error("리포트 생성 실패: {}", targetDate, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 오늘 리포트 수동 생성 (테스트용)
     * POST /api/reports/generate/today
     */
    @PostMapping("/generate/today")
    public ResponseEntity<DailyReportResponse> generateTodayReport() {
        LocalDate today = LocalDate.now();
        log.info("오늘 리포트 수동 생성 요청: {}", today);

        try {
            DailyReport report = dailyReportService.generateDailyReport(today);

            if (report != null) {
                DailyReportResponse response = DailyReportResponse.fromEntity(report);
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(null);
            }
        } catch (Exception e) {
            log.error("오늘 리포트 생성 실패: {}", today, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 리포트 존재 여부 확인
     * GET /api/reports/exists?date=2025-12-09
     */
    @GetMapping("/exists")
    public ResponseEntity<Boolean> checkReportExists(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date
    ) {
        log.info("리포트 존재 여부 확인: {}", date);

        Optional<DailyReport> report = dailyReportService.getReportByDate(date);
        return ResponseEntity.ok(report.isPresent());
    }

    /**
     * 헬스 체크
     * GET /api/reports/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Daily Report API is running");
    }
}
