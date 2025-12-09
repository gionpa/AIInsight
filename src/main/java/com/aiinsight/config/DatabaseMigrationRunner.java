package com.aiinsight.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 데이터베이스 마이그레이션 확인 러너
 * - pgvector 서비스 연결 확인
 * - 테이블 존재 여부 확인
 * - pgvector 확장 확인
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseMigrationRunner implements CommandLineRunner {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final Environment environment;

    @Override
    public void run(String... args) throws Exception {
        // railway 프로파일에서만 실행
        if (!isRailwayProfile()) {
            log.info("Railway 프로파일이 아니므로 마이그레이션 확인을 건너뜁니다.");
            return;
        }

        log.info("====================================");
        log.info("데이터베이스 마이그레이션 확인 시작");
        log.info("====================================");

        try (Connection conn = dataSource.getConnection()) {
            log.info("✅ 데이터베이스 연결 성공");
            log.info("   URL: {}", conn.getMetaData().getURL());
            log.info("   User: {}", conn.getMetaData().getUserName());

            // 테이블 목록 조회
            List<String> tables = getTableList(conn);
            log.info("✅ 테이블 수: {}", tables.size());

            if (!tables.isEmpty()) {
                log.info("   테이블 목록:");
                for (String table : tables) {
                    log.info("   - {}", table);
                }
            }

            // pgvector 확장 확인
            checkPgVectorExtension(conn);

            // 주요 테이블 데이터 확인
            checkTableData();

            log.info("====================================");
            log.info("데이터베이스 마이그레이션 확인 완료");
            log.info("====================================");

        } catch (Exception e) {
            log.error("❌ 데이터베이스 연결 실패: {}", e.getMessage(), e);
            throw e;
        }
    }

    private boolean isRailwayProfile() {
        String[] activeProfiles = environment.getActiveProfiles();
        for (String profile : activeProfiles) {
            if ("railway".equals(profile)) {
                return true;
            }
        }
        return false;
    }

    private List<String> getTableList(Connection conn) throws Exception {
        List<String> tables = new ArrayList<>();
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                "SELECT table_name FROM information_schema.tables " +
                "WHERE table_schema = 'public' AND table_type = 'BASE TABLE' " +
                "ORDER BY table_name"
            );
            while (rs.next()) {
                tables.add(rs.getString("table_name"));
            }
        }
        return tables;
    }

    private void checkPgVectorExtension(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                "SELECT extname, extversion FROM pg_extension WHERE extname = 'vector'"
            );
            if (rs.next()) {
                log.info("✅ pgvector 확장 활성화됨");
                log.info("   버전: {}", rs.getString("extversion"));
            } else {
                log.warn("⚠️  pgvector 확장이 활성화되지 않음");
            }
        }
    }

    private void checkTableData() {
        try {
            // news_article 테이블 확인
            Integer articleCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM news_article", Integer.class
            );
            log.info("✅ news_article: {} 개", articleCount);

            // crawl_target 테이블 확인
            Integer targetCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM crawl_target", Integer.class
            );
            log.info("✅ crawl_target: {} 개", targetCount);

        } catch (Exception e) {
            log.warn("⚠️  테이블 데이터 확인 중 오류: {}", e.getMessage());
        }
    }
}
