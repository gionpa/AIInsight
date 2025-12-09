package com.aiinsight.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * pgvector 확장 초기화 설정
 * - Railway PostgreSQL에서 pgvector 확장 활성화
 * - 임베딩 벡터 기반 유사도 검색 지원
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class PgVectorConfig {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void initPgVector() {
        try {
            // pgvector 확장 사용 가능 여부 확인
            String checkQuery = "SELECT COUNT(*) FROM pg_available_extensions WHERE name = 'vector'";
            Integer available = jdbcTemplate.queryForObject(checkQuery, Integer.class);

            if (available != null && available > 0) {
                log.info("✓ pgvector 확장 사용 가능");

                // pgvector 확장 활성화
                jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
                log.info("✓ pgvector 확장 활성화 완료");

                // 설치된 버전 확인
                String versionQuery = "SELECT extversion FROM pg_extension WHERE extname = 'vector'";
                try {
                    String version = jdbcTemplate.queryForObject(versionQuery, String.class);
                    log.info("✓ pgvector 버전: {}", version);
                } catch (Exception e) {
                    log.warn("pgvector 버전 확인 실패: {}", e.getMessage());
                }
            } else {
                log.warn("✗ pgvector 확장을 사용할 수 없습니다. Railway 플랜을 확인하세요.");
            }
        } catch (Exception e) {
            log.error("pgvector 초기화 실패: {}", e.getMessage());
            log.warn("임베딩 벡터 기능이 비활성화됩니다.");
        }
    }
}
