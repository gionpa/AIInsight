-- BGE 모델 (384 차원)로 변경하기 위한 마이그레이션
-- 기존 OpenAI 모델 (1536 차원)에서 BAAI/bge-small-en-v1.5 (384 차원)로 변경

-- 1. 기존 임베딩 데이터 모두 삭제 (차원이 다르므로 재생성 필요)
TRUNCATE TABLE article_embedding;

-- 2. embedding_vector 컬럼 타입 변경: vector(1536) -> vector(384)
ALTER TABLE article_embedding
ALTER COLUMN embedding_vector TYPE vector(384);

-- 3. 인덱스 재생성 (벡터 차원이 변경되었으므로)
DROP INDEX IF EXISTS idx_article_embedding_vector;
CREATE INDEX idx_article_embedding_vector ON article_embedding
USING ivfflat (embedding_vector vector_cosine_ops)
WITH (lists = 100);
