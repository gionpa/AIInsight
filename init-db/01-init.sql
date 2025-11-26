-- AI Insight 초기 데이터베이스 설정

-- 확장 기능 활성화
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 인덱스 생성을 위한 설정
SET timezone = 'Asia/Seoul';

-- 초기 크롤링 타겟 샘플 데이터 (선택사항)
-- INSERT INTO crawl_target (name, url, description, selector_config, cron_expression, enabled, crawl_type, created_at, updated_at)
-- VALUES
-- ('TechCrunch AI', 'https://techcrunch.com/category/artificial-intelligence/', 'TechCrunch AI 뉴스',
--  '{"articleItemSelector": "article", "titleSelector": "h2 a", "linkSelector": "h2 a"}',
--  '0 0 */2 * * *', true, 'STATIC', NOW(), NOW());
