-- pgvector 확장 활성화
-- Railway PostgreSQL에서 pgvector 지원 여부 확인 및 활성화

CREATE EXTENSION IF NOT EXISTS vector;

-- pgvector 확장 정보 확인용 뷰
CREATE OR REPLACE VIEW pgvector_info AS
SELECT
    extname as extension_name,
    extversion as version,
    (SELECT comment FROM pg_description WHERE objoid = pe.oid) as description
FROM pg_extension pe
WHERE extname = 'vector';

-- 설치 확인 로그
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'vector') THEN
        RAISE NOTICE 'pgvector 확장이 성공적으로 활성화되었습니다.';
    ELSE
        RAISE WARNING 'pgvector 확장을 활성화할 수 없습니다. Railway 플랜을 확인하세요.';
    END IF;
END $$;
