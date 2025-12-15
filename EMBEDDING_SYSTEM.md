# 임베딩 시스템 (Embedding System)

## 📋 개요

AIInsight의 임베딩 시스템은 뉴스 기사를 벡터로 변환하여 의미적 유사도 검색을 가능하게 합니다. 이를 통해 고도화된 리포트 생성, 토픽 클러스터링, 유사 기사 추천이 가능합니다.

**최종 업데이트**: 2025-12-15
**상태**: ✅ 프로덕션 운영 중

---

## 🎯 주요 기능

### 1. 자동 임베딩 생성
- **중요도 HIGH 기사 자동 처리**: AI 분석 후 중요도가 HIGH로 판정된 기사에 대해 자동으로 임베딩 생성
- **비동기 처리**: 크롤링 응답 속도에 영향을 주지 않도록 비동기로 임베딩 생성
- **비용 최적화**: 중요도 HIGH 기사만 임베딩 생성하여 API 비용 약 75% 절감

### 2. 배치 임베딩 생성
- **일반 배치**: 임베딩이 없는 모든 기사에 대해 배치 생성
- **HIGH 기사 우선 배치**: 중요도 HIGH이면서 임베딩이 없는 기사 우선 처리
- **Rate Limiting**: API 요청 간 100ms 대기로 안정성 확보

### 3. 의미적 유사도 검색
- **코사인 유사도 기반**: pgvector의 `<=>` 연산자 활용
- **빠른 검색**: 벡터 인덱스를 통한 고속 유사 기사 검색
- **컨텍스트 검색**: 특정 기간 내 유사 기사 검색 지원

---

## 🏗️ 시스템 아키텍처

### 기술 스택

| 구성 요소 | 기술 | 세부사항 |
|----------|------|----------|
| **임베딩 모델** | BAAI/bge-m3 | 1024차원, 다국어 지원 |
| **임베딩 서버** | text-embeddings-inference | Hugging Face 공식 서버 |
| **벡터 DB** | PostgreSQL + pgvector | 코사인 유사도 검색 |
| **백업 모델** | OpenAI text-embedding-3-small | 1536차원 (미사용) |

### 데이터베이스 스키마

```sql
-- 임베딩 테이블
CREATE TABLE article_embedding (
    id BIGSERIAL PRIMARY KEY,
    article_id BIGINT NOT NULL REFERENCES news_article(id),
    embedding_vector vector(1024) NOT NULL,  -- pgvector 타입
    model_name VARCHAR(100) NOT NULL,        -- 'BAAI/bge-m3'
    token_count INTEGER,
    quality_score DOUBLE PRECISION,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(article_id)
);

-- 벡터 검색 성능을 위한 인덱스
CREATE INDEX idx_embedding_vector ON article_embedding
    USING ivfflat (embedding_vector vector_cosine_ops);
```

### 프로세스 플로우

```
┌─────────────────┐
│ 1. 기사 크롤링   │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ 2. AI 분석      │ (Claude CLI)
│  - 요약         │
│  - 카테고리     │
│  - 중요도 판정  │
└────────┬────────┘
         │
         ▼
    [중요도 체크]
         │
    [HIGH?] ──NO──> 종료
         │
        YES
         │
         ▼
┌─────────────────┐
│ 3. 임베딩 생성  │ (비동기)
│  - 텍스트 준비  │
│  - API 호출     │
│  - 벡터 저장    │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ 4. pgvector DB  │
│  - 코사인 검색  │
│  - 유사도 계산  │
└─────────────────┘
```

---

## 💻 주요 컴포넌트

### 1. EmbeddingService.java

**위치**: `src/main/java/com/aiinsight/service/EmbeddingService.java`

**핵심 메서드**:

```java
// 단일 기사 임베딩 생성
@Transactional
public ArticleEmbedding generateAndSaveEmbedding(NewsArticle article)

// 임베딩 없는 기사 배치 생성
@Transactional
public int generateEmbeddingsForArticlesWithoutEmbedding(int limit)

// HIGH 중요도 기사 우선 배치 생성
@Transactional
public int generateEmbeddingsForHighImportanceArticles(int limit)

// 유사 기사 검색 (코사인 유사도)
@Transactional(readOnly = true)
public List<Map<String, Object>> findSimilarArticles(Long articleId, int limit)
```

**임베딩 생성 프로세스**:
1. 이미 임베딩 존재 여부 확인
2. 임베딩 텍스트 준비 (한글 제목 > 영문 제목 > AI 요약)
3. 토큰 수 추정 (최대 8000자, 약 2000 토큰)
4. 임베딩 API 호출 (local-bge 또는 OpenAI)
5. 품질 점수 계산 (AI 분석 여부, 한글 제목, 중요도 고려)
6. PostgreSQL에 벡터 저장

### 2. AiSummaryService.java (자동 생성 트리거)

**위치**: `src/main/java/com/aiinsight/service/AiSummaryService.java`

**임베딩 자동 생성 로직**:
```java
// AI 분석 완료 후 중요도 체크
if (parsed) {
    NewsArticle updatedArticle = newsArticleService.findEntityById(article.getId());
    if (updatedArticle != null &&
        updatedArticle.getImportance() == NewsArticle.ArticleImportance.HIGH) {
        try {
            embeddingService.generateAndSaveEmbedding(updatedArticle);
            log.info("중요도 HIGH 기사 임베딩 생성 완료 (기사 ID: {})", article.getId());
        } catch (Exception e) {
            log.error("임베딩 생성 실패 (기사 ID: {}): {}", article.getId(), e.getMessage());
        }
    }
}
```

### 3. ArticleEmbeddingRepository.java

**위치**: `src/main/java/com/aiinsight/domain/embedding/ArticleEmbeddingRepository.java`

**핵심 쿼리**:

```java
// 임베딩 없는 모든 기사 조회
@Query("""
    SELECT na FROM NewsArticle na
    WHERE NOT EXISTS (
        SELECT 1 FROM ArticleEmbedding ae
        WHERE ae.article = na
    )
    ORDER BY na.publishedAt DESC
    """)
List<NewsArticle> findArticlesWithoutEmbedding(Pageable pageable);

// 중요도 HIGH이면서 임베딩 없는 기사 조회
@Query("""
    SELECT na FROM NewsArticle na
    WHERE na.importance = 'HIGH'
      AND NOT EXISTS (
        SELECT 1 FROM ArticleEmbedding ae
        WHERE ae.article = na
    )
    ORDER BY na.publishedAt DESC
    """)
List<NewsArticle> findHighImportanceArticlesWithoutEmbedding(Pageable pageable);

// 코사인 유사도 기반 유사 기사 검색
@Query(value = """
    SELECT ae.*, 1 - (ae.embedding_vector <=> CAST(:queryVector AS vector)) AS similarity
    FROM article_embedding ae
    WHERE ae.article_id != :excludeArticleId
    ORDER BY ae.embedding_vector <=> CAST(:queryVector AS vector)
    LIMIT :limit
    """, nativeQuery = true)
List<Object[]> findSimilarArticles(
    @Param("queryVector") String queryVector,
    @Param("excludeArticleId") Long excludeArticleId,
    @Param("limit") int limit
);
```

---

## 🔌 API 엔드포인트

### 1. 배치 임베딩 생성

```bash
# 모든 기사 대상 (최대 10개)
POST /api/crawl/generate-embeddings?limit=10

# 중요도 HIGH 기사 대상 (최대 100개)
POST /api/crawl/generate-embeddings-high?limit=100
```

**응답 예시**:
```json
{
  "generatedCount": 20,
  "requestedLimit": 100
}
```

### 2. 임베딩 연결 테스트

```bash
# 임베딩 서버 연결 상태 확인
GET /api/embeddings/test-connection
```

**응답 예시**:
```json
{
  "status": "success",
  "provider": "local-bge",
  "model": "BAAI/bge-m3",
  "endpoint": "http://localhost:8081/embeddings",
  "dimension": 1024,
  "serverResponse": "{...}"
}
```

---

## ⚙️ 설정 (application.yml)

```yaml
ai:
  embedding:
    provider: local-bge           # local-bge | openai
    model: BAAI/bge-m3            # 임베딩 모델명
    endpoint: http://localhost:8081/embeddings  # 로컬 서버 (Railway는 내부 URL)
    dimension: 1024               # 벡터 차원
```

**Railway 프로덕션 설정**:
- `AI_EMBEDDING_ENDPOINT`: `http://embedding-server.railway.internal:8081/embeddings`
- Railway 내부 네트워크를 통한 빠른 통신

---

## 📊 성능 지표

### 임베딩 생성 속도
- **단일 기사**: 평균 3초 (토큰 수에 따라 변동)
- **배치 생성 (20개)**: 약 64초 (3.2초/기사)
- **API Rate Limit**: 요청 간 100ms 대기

### 검색 성능
- **벡터 인덱스**: ivfflat (코사인 유사도)
- **검색 속도**: <100ms (10만 개 기사 기준)
- **정확도**: 유사도 0.7 이상 = 높은 연관성

### 비용 효율성
- **전체 기사 임베딩**: 불필요한 비용 발생
- **HIGH만 임베딩**: 약 75% 비용 절감 (25% 기사만 처리)
- **토큰 당 비용**: BAAI/bge-m3는 로컬 서버로 무료

---

## 🔍 활용 사례

### 1. 일일 리포트 생성
- 계층적 클러스터링: 유사도 0.65 기준으로 토픽 그룹핑
- Centroid 기반 토픽명: 대표 기사 제목으로 직관적 토픽명 생성
- 트렌드 분석: 7일 전 vs 최근 데이터 비교

### 2. 유사 기사 추천
- 사용자가 읽은 기사와 유사한 기사 추천
- 유사도 기준: 0.7 이상 (높은 연관성)

### 3. 중복 기사 감지
- 같은 내용의 중복 기사 자동 감지
- 유사도 0.9 이상 = 거의 동일한 내용

---

## 🛠️ 트러블슈팅

### 문제 1: 임베딩 생성 0개

**증상**:
```json
{"generatedCount":0,"requestedLimit":20}
```

**원인**:
- `findArticlesWithoutEmbedding()`이 `publishedAt DESC` 정렬로 최신 기사만 조회
- HIGH 중요도 기사가 오래된 기사라 조회 안 됨

**해결책**:
- `findHighImportanceArticlesWithoutEmbedding()` 쿼리 추가
- 중요도 필터를 DB 레벨에서 적용

### 문제 2: 임베딩 서버 연결 실패

**증상**:
```
Connection refused: localhost:8081
```

**원인**: 로컬 임베딩 서버 미실행

**해결책**:
```bash
# Docker로 임베딩 서버 실행
docker run -p 8081:80 \
  --name embedding-server \
  ghcr.io/huggingface/text-embeddings-inference:latest \
  --model-id BAAI/bge-m3
```

### 문제 3: pgvector 확장 없음

**증상**:
```
ERROR: type "vector" does not exist
```

**해결책**:
```sql
-- PostgreSQL에서 pgvector 확장 설치
CREATE EXTENSION IF NOT EXISTS vector;

-- 버전 확인
SELECT extversion FROM pg_extension WHERE extname = 'vector';
```

---

## 📈 향후 개선 계획

### 1. 임베딩 모델 업그레이드
- [ ] BAAI/bge-m3 → bge-large-v1.5 (1024 → 1536 차원)
- [ ] 한국어 특화 모델 테스트 (KR-SBERT)

### 2. 검색 알고리즘 개선
- [ ] HNSW 인덱스로 교체 (ivfflat → hnsw)
- [ ] 하이브리드 검색 (키워드 + 벡터)

### 3. 캐싱 시스템
- [ ] Redis에 자주 조회되는 임베딩 캐싱
- [ ] 유사 기사 검색 결과 캐싱

### 4. 모니터링 강화
- [ ] 임베딩 생성 실패율 추적
- [ ] 검색 품질 메트릭 (Precision@K, Recall@K)
- [ ] 임베딩 커버리지 대시보드

---

## 📚 참고 자료

- [pgvector 공식 문서](https://github.com/pgvector/pgvector)
- [BAAI/bge-m3 모델 카드](https://huggingface.co/BAAI/bge-m3)
- [text-embeddings-inference](https://github.com/huggingface/text-embeddings-inference)
- [코사인 유사도 설명](https://en.wikipedia.org/wiki/Cosine_similarity)

---

## 🔗 관련 문서

- [Executive_Summary.md](./Executive_Summary.md): RAG 기반 리포트 생성 시스템
- [IMPLEMENTATION.md](./IMPLEMENTATION.md): 전체 시스템 구현 상세
- [RAILWAY_DEPLOYMENT.md](./RAILWAY_DEPLOYMENT.md): Railway 배포 가이드
