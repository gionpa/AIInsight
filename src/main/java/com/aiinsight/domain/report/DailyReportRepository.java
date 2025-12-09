package com.aiinsight.domain.report;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyReportRepository extends JpaRepository<DailyReport, Long> {

    /**
     * 날짜로 리포트 조회
     */
    Optional<DailyReport> findByReportDate(LocalDate reportDate);

    /**
     * 날짜 범위로 리포트 조회
     */
    List<DailyReport> findByReportDateBetweenOrderByReportDateDesc(LocalDate startDate, LocalDate endDate);

    /**
     * 상태별 리포트 조회
     */
    List<DailyReport> findByStatus(DailyReport.ReportStatus status);

    /**
     * 완료된 리포트 목록 (최신순)
     */
    Page<DailyReport> findByStatusOrderByReportDateDesc(DailyReport.ReportStatus status, Pageable pageable);

    /**
     * 최근 N일간의 완료된 리포트 조회
     */
    @Query("SELECT dr FROM DailyReport dr WHERE dr.reportDate >= :startDate AND dr.status = :status ORDER BY dr.reportDate DESC")
    List<DailyReport> findRecentReports(
            @Param("startDate") LocalDate startDate,
            @Param("status") DailyReport.ReportStatus status
    );

    /**
     * 특정 날짜 리포트 존재 여부
     */
    boolean existsByReportDate(LocalDate reportDate);

    /**
     * 품질 점수 기준 이상의 리포트 조회
     */
    List<DailyReport> findByQualityScoreGreaterThanEqualOrderByReportDateDesc(Double minQualityScore);

    /**
     * 최신 리포트 조회
     */
    Optional<DailyReport> findFirstByStatusOrderByReportDateDesc(DailyReport.ReportStatus status);

    /**
     * 생성 실패한 리포트 목록
     */
    List<DailyReport> findByStatusOrderByReportDateDesc(DailyReport.ReportStatus status);

    /**
     * 평균 기사 수 조회
     */
    @Query("SELECT AVG(dr.totalArticles) FROM DailyReport dr WHERE dr.status = :status")
    Double getAverageTotalArticles(@Param("status") DailyReport.ReportStatus status);

    /**
     * 평균 품질 점수 조회
     */
    @Query("SELECT AVG(dr.qualityScore) FROM DailyReport dr WHERE dr.status = :status AND dr.qualityScore IS NOT NULL")
    Double getAverageQualityScore(@Param("status") DailyReport.ReportStatus status);

    /**
     * 월별 리포트 개수 조회
     */
    @Query(value = """
        SELECT
            DATE_TRUNC('month', report_date) as month,
            COUNT(*) as report_count
        FROM daily_report
        WHERE status = :status
        GROUP BY DATE_TRUNC('month', report_date)
        ORDER BY month DESC
        """, nativeQuery = true)
    List<Object[]> getMonthlyReportCounts(@Param("status") String status);
}
