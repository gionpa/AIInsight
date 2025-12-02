# AIInsight 구현 내역

## 프로젝트 개요

AI 뉴스 자동 수집 및 분석 시스템
- **백엔드**: Spring Boot 3.2 + Java 21 + H2 Database
- **프론트엔드**: React 18 + TypeScript + Vite + TailwindCSS
- **AI 분석**: Claude CLI (headless mode)
- **크롤링**: Jsoup (정적), Selenium (동적)

---

## 주요 기능

### 1. 뉴스 크롤링 시스템

#### 크롤 타겟 관리
- CRUD 기능 (생성, 조회, 수정, 삭제)
- 크론 스케줄링 지원 (자동 크롤링)
- 정적/동적 웹 크롤링 선택 가능
- CSS 셀렉터 기반 커스텀 크롤링 설정

**주요 파일**:
- `CrawlTarget.java` - 크롤 타겟 엔티티
- `WebCrawler.java` - 크롤링 로직
- `CrawlScheduler.java` - 스케줄러
- `CrawlTargets.tsx` - 프론트엔드 UI

#### 크롤링 이력 관리
- 크롤링 성공/실패 이력 기록
- 실행 시간, 발견 기사 수, 에러 메시지 추적
- 페이지네이션 지원

### 2. AI 기사 분석

#### Claude CLI 통합
- **Headless 모드** 사용으로 자동화
- 기사 제목 및 내용 분석
- 배치 분석 지원 (한 번에 여러 기사 처리)

#### 분석 항목
1. **titleKo (한글 제목 번역)**
   - 원문 영어 제목을 자연스러운 한국어로 번역
   - 리포트 및 기사 목록에서 한글 제목 우선 표시

2. **summary (한국어 요약)**
   - 3-5문장으로 핵심 내용 요약
   - 한국어로 작성

3. **relevanceScore (AI 관련성 점수)**
   - 0.0 ~ 1.0 사이 점수
   - AI 분야와의 관련도 평가

4. **category (카테고리 분류)**
   - LLM (대규모 언어 모델)
   - COMPUTER_VISION (컴퓨터 비전)
   - NLP (자연어 처리)
   - ROBOTICS (로보틱스)
   - ML_OPS (MLOps)
   - RESEARCH (연구)
   - INDUSTRY (산업)
   - STARTUP (스타트업)
   - REGULATION (규제/정책)
   - TUTORIAL (튜토리얼)
   - PRODUCT (제품)
   - OTHER (기타)

5. **importance (중요도)**
   - HIGH: 업계 전반에 영향을 미치는 중요한 뉴스
   - MEDIUM: 특정 분야에 유의미한 뉴스
   - LOW: 일반적인 소식

**주요 파일**:
- `AiSummaryService.java` - AI 분석 서비스
- `NewsArticle.java` - 기사 엔티티 (titleKo 필드 포함)
- `NewsArticleService.java` - 기사 관리 서비스

### 3. 리포트 시스템

#### 일일 리포트
- HIGH 중요도 기사만 필터링
- Executive Summary 자동 생성
- 카테고리별 분포 통계
- 중요 기사 목록 (한글 제목 + 원문 제목)

#### 카테고리별 리포트
- 카테고리별 그룹핑
- 각 카테고리별 기사 수 표시
- 접기/펼치기 기능

**주요 파일**:
- `ReportService.java` - 리포트 생성 로직
- `ReportDto.java` - 리포트 DTO (ArticleSummary 포함)
- `Report.tsx` - 리포트 UI

### 4. 프론트엔드 UI

#### Dashboard
- 전체 통계 현황
- 최근 크롤링 이력
- 카테고리별 분포 차트

#### Articles (뉴스 기사)
- 페이지네이션 지원
- 키워드 검색 기능
- 기사 상세보기 모달
- NEW 배지 표시
- **날짜 및 시간 정보 표시** (YYYY. MM. DD. HH:MM)
- 카테고리, 중요도, 관련성 점수 표시

#### Report (리포트)
- 일일/카테고리별 뷰 전환
- **한글 제목 우선 표시** (원문 제목은 작은 이탤릭체로 하단 표시)
- **날짜 및 시간 정보 표시** (YYYY. MM. DD. HH:MM)
- 요약 접기/펼치기
- Executive Summary 강조 표시

#### Crawl Targets
- 크롤 타겟 CRUD
- 즉시 크롤링 실행
- 활성화/비활성화 토글

#### Crawl History
- 크롤링 이력 조회
- 성공/실패/부분성공 상태 표시
- 실행 시간 및 기사 수 통계

---

## 데이터베이스 스키마

### news_article 테이블
```sql
- id: BIGINT (PK)
- target_id: BIGINT (FK)
- original_url: VARCHAR(2048)
- title: VARCHAR(500) -- 원문 제목
- title_ko: VARCHAR(500) -- 한글 번역 제목 ⭐ NEW
- content: TEXT
- summary: TEXT -- AI 요약
- author: VARCHAR(255)
- published_at: TIMESTAMP
- relevance_score: DOUBLE -- AI 관련성 점수
- category: VARCHAR(50) -- AI 카테고리
- importance: VARCHAR(10) -- HIGH/MEDIUM/LOW
- is_new: BOOLEAN
- is_summarized: BOOLEAN
- thumbnail_url: VARCHAR(1024)
- content_hash: VARCHAR(64)
- crawled_at: TIMESTAMP
- updated_at: TIMESTAMP
```

### crawl_target 테이블
```sql
- id: BIGINT (PK)
- name: VARCHAR(255)
- url: VARCHAR(2048)
- description: TEXT
- selector_config: TEXT
- cron_expression: VARCHAR(100)
- crawl_type: VARCHAR(20) -- STATIC/DYNAMIC
- enabled: BOOLEAN
- last_crawled_at: TIMESTAMP
- last_status: VARCHAR(20)
- created_at: TIMESTAMP
- updated_at: TIMESTAMP
```

### crawl_history 테이블
```sql
- id: BIGINT (PK)
- target_id: BIGINT (FK)
- status: VARCHAR(20) -- SUCCESS/FAILED/PARTIAL
- articles_found: INT
- articles_new: INT
- duration_ms: BIGINT
- error_message: TEXT
- executed_at: TIMESTAMP
```

---

## API 엔드포인트

### Dashboard
- `GET /api/dashboard/stats` - 전체 통계

### Crawl Targets
- `GET /api/crawl-targets` - 타겟 목록
- `POST /api/crawl-targets` - 타겟 생성
- `GET /api/crawl-targets/{id}` - 타겟 상세
- `PUT /api/crawl-targets/{id}` - 타겟 수정
- `DELETE /api/crawl-targets/{id}` - 타겟 삭제

### Crawl
- `POST /api/crawl/execute/{targetId}` - 특정 타겟 크롤링
- `POST /api/crawl/execute-all` - 전체 크롤링

### Articles
- `GET /api/articles` - 기사 목록
- `GET /api/articles/{id}` - 기사 상세
- `GET /api/articles/search?keyword={keyword}` - 키워드 검색
- `POST /api/articles/{id}/analyze` - 단일 기사 AI 분석
- `POST /api/articles/analyze-batch?limit={limit}` - 배치 분석
- `POST /api/articles/{id}/mark-read` - 읽음 처리

### Reports
- `GET /api/reports/daily` - 일일 리포트
- `GET /api/reports/by-category` - 카테고리별 리포트
- `GET /api/reports/category/{category}` - 특정 카테고리 리포트

### Scheduler
- `POST /api/scheduler/refresh` - 스케줄러 갱신
- `GET /api/scheduler/status` - 스케줄러 상태

---

## 최근 구현 사항 (2025-11-26)

### 1. 한글 제목 번역 기능
**목적**: 영어 기사 제목을 한국어로 번역하여 가독성 향상

**구현 내용**:
- `NewsArticle` 엔티티에 `titleKo` 필드 추가
- AI 프롬프트에 `titleKo` 번역 요청 추가
- `ReportDto.ArticleSummary`에서 한글 제목 우선 표시 로직 구현
- 프론트엔드에서 한글 제목을 크게, 원문 제목을 작게 표시

**변경 파일**:
- `NewsArticle.java` - titleKo 컬럼 추가
- `AiSummaryService.java` - 프롬프트 수정, titleKo 파싱
- `NewsArticleService.java` - updateSummary에 titleKo 파라미터 추가
- `ReportDto.java` - title/originalTitle 로직 구현
- `Report.tsx` - 한글/원문 제목 구분 표시
- `types/index.ts` - ArticleSummary 타입 업데이트

### 2. 날짜 및 시간 정보 표시
**목적**: 기사의 정확한 크롤링 시간 정보 제공

**구현 내용**:
- 기존: `toLocaleDateString()` → 날짜만 표시
- 변경: `toLocaleString('ko-KR', {...})` → 날짜 + 시간 표시
- 포맷: YYYY. MM. DD. HH:MM

**변경 파일**:
- `Report.tsx` - crawledAt 표시 형식 변경
- `Articles.tsx` - crawledAt 및 publishedAt 표시 형식 변경

---

## 실행 방법

### 백엔드 실행
```bash
cd /Users/user/claude_projects/AIInsight
./gradlew bootRun

# 또는 빌드 후 실행
./gradlew build
java -jar build/libs/aiinsight-0.0.1-SNAPSHOT.jar
```

### 프론트엔드 실행
```bash
cd /Users/user/claude_projects/AIInsight/frontend
npm install
npm run dev

# 프로덕션 빌드
npm run build
```

### Docker 실행 (PostgreSQL)
```bash
# 데이터베이스 시작
./start-db.sh

# 또는 docker-compose 직접 사용
docker-compose up -d

# 데이터베이스 중지
./stop-db.sh
```

---

## 환경 변수

### application.yml
```yaml
spring:
  datasource:
    url: jdbc:h2:file:./data/aiinsight
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: update

ai:
  claude:
    command: claude --headless
```

---

## 프로젝트 구조

```
AIInsight/
├── src/main/java/com/aiinsight/
│   ├── config/           # 설정 (CORS, Scheduler 등)
│   ├── controller/       # REST API 컨트롤러
│   ├── crawler/          # 웹 크롤링 로직
│   ├── domain/           # JPA 엔티티
│   │   ├── article/      # 기사 관련
│   │   └── crawl/        # 크롤 타겟/이력
│   ├── dto/              # DTO 클래스
│   ├── scheduler/        # 스케줄러
│   └── service/          # 비즈니스 로직
│       ├── AiSummaryService.java
│       ├── NewsArticleService.java
│       ├── ReportService.java
│       └── ...
├── frontend/
│   └── src/
│       ├── api/          # API 클라이언트
│       ├── components/   # 공통 컴포넌트
│       ├── pages/        # 페이지 컴포넌트
│       │   ├── Articles.tsx
│       │   ├── Report.tsx
│       │   ├── Dashboard.tsx
│       │   ├── CrawlTargets.tsx
│       │   └── CrawlHistory.tsx
│       └── types/        # TypeScript 타입 정의
├── data/                 # H2 데이터베이스 파일
├── init-db/              # 초기 데이터 SQL
├── docker-compose.yml    # PostgreSQL 설정
└── README.md
```

---

## 향후 개선 사항

### 기능 개선
- [ ] 사용자 인증/권한 관리
- [ ] 기사 북마크 기능
- [ ] 키워드 알림 설정
- [ ] RSS 피드 지원
- [ ] 기사 소셜 공유 기능

### 성능 최적화
- [ ] 크롤링 병렬화
- [ ] AI 분석 배치 크기 최적화
- [ ] 데이터베이스 인덱싱 개선
- [ ] 프론트엔드 코드 스플리팅

### 확장성
- [ ] PostgreSQL 완전 전환
- [ ] Redis 캐싱 도입
- [ ] 분산 크롤링 아키텍처
- [ ] Kafka를 통한 이벤트 스트리밍

### UI/UX
- [ ] 다크 모드 지원
- [ ] 모바일 반응형 개선
- [ ] 키보드 단축키
- [ ] 오프라인 지원 (PWA)

---

## 트러블슈팅

### Claude CLI 권한 에러
```bash
chmod +x /usr/local/bin/claude
```

### H2 데이터베이스 락 에러
```bash
# 데이터베이스 파일 삭제 후 재시작
rm -rf ./data/aiinsight*
```

### Selenium WebDriver 에러
```bash
# ChromeDriver 설치
brew install chromedriver
```

---

## 라이선스

This project is private and proprietary.

---

## 작성자

- Claude Code (AI Assistant)
- 개발 기간: 2025-11-26

---

## 변경 이력

### 2025-12-02
- ✅ 뉴스 기사 중요도 필터링 기능 추가
- ✅ 메타데이터 추출 정규식 보강 (og:title, og:description)
- ✅ 기사 원문 자동 업데이트 로직 추가
- ✅ 일반적인 제목 감지 기능 추가 (isGenericTitle)

### 2025-11-26
- ✅ 한글 제목 번역 기능 구현 (titleKo)
- ✅ 날짜 및 시간 정보 표시 추가
- ✅ Git 저장소 초기화 및 커밋
- ✅ 구현 문서 작성

---

## AI 기사 분석 시스템 상세 스펙

### 개요

AI 기사 분석 시스템은 수집된 뉴스 기사에 대해 다음 작업을 자동으로 수행합니다:
1. **메타데이터 추출**: URL에서 og:title, og:description 등 추출
2. **한글 제목 번역**: 영어 제목을 자연스러운 한국어로 번역
3. **내용 요약**: 기사 핵심 내용을 3-5문장으로 요약
4. **카테고리 분류**: AI 관련 12개 카테고리로 자동 분류
5. **중요도 평가**: HIGH/MEDIUM/LOW 3단계 중요도 판정
6. **관련성 점수**: AI 분야와의 관련도 0.0~1.0 점수 부여

### 아키텍처

```
┌─────────────────────────────────────────────────────────────────┐
│                      AiSummaryService.java                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐       │
│  │ 메타데이터    │ → │ AI 분석      │ → │ 결과 저장    │       │
│  │ 추출         │    │ (Claude CLI) │    │ (DB)        │       │
│  └──────────────┘    └──────────────┘    └──────────────┘       │
│         │                   │                   │                │
│         ▼                   ▼                   ▼                │
│  fetchMetadataFromUrl() → callClaudeCli() → updateSummary()     │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 메타데이터 추출 시스템

#### 1. URL 메타데이터 추출 (`fetchMetadataFromUrl`)

기사 URL에서 OpenGraph 메타데이터를 추출합니다.

**추출 우선순위**:
1. `og:title` (OpenGraph 제목)
2. `og:description` (OpenGraph 설명)
3. `<title>` 태그 (폴백)
4. `name="description"` 메타 태그 (폴백)

**정규식 패턴** (속성 순서 무관):
```java
// 패턴 1: property="og:title" content="..."
Pattern.compile("<meta\\s+property=[\"']og:title[\"']\\s+content=[\"']([^\"']+)[\"']")

// 패턴 2: content="..." property="og:title"
Pattern.compile("<meta\\s+content=[\"']([^\"']+)[\"']\\s+property=[\"']og:title[\"']")
```

**HTML 엔티티 디코딩**:
- `&amp;` → `&`
- `&lt;` → `<`
- `&gt;` → `>`
- `&quot;` → `"`
- `&#39;`, `&apos;`, `&#x27;` → `'`
- `&hellip;` → `...`
- `&nbsp;` → ` `

#### 2. 일반적인 제목 감지 (`isGenericTitle`)

의미 없는 제목을 감지하여 메타데이터 재추출을 트리거합니다.

**감지 패턴**:
```java
String[] genericPatterns = {
    "기사 상세", "기사 보기", "상세 보기",
    "article view", "article detail", "view article",
    "untitled", "no title", "제목 없음",
    "loading", "로딩"
};
```

**추가 조건**:
- 제목 길이 5자 미만

#### 3. URL 경로에서 제목 추출 (`extractTitleFromUrl`)

메타데이터 추출 실패 시 URL 경로에서 제목을 추측합니다.

```
https://example.com/news/ai-breakthrough-2025
→ "Ai breakthrough 2025"
```

**처리 과정**:
1. 쿼리 파라미터 제거 (`?` 이후)
2. 마지막 경로 세그먼트 추출
3. 확장자 제거 (`.html`, `.php` 등)
4. 하이픈/언더스코어를 공백으로 변환
5. 긴 숫자 제거 (날짜 등)
6. 첫 글자 대문자화

### AI 분석 프로세스

#### 1. 분석 트리거 조건

```java
public void summarizeArticle(NewsArticle article, boolean forceReanalyze) {
    // 이미 분석된 경우 스킵 (force가 아닌 경우)
    if (!forceReanalyze && article.getIsSummarized()) {
        return;
    }
    // ...
}
```

#### 2. 제목/본문 보강 로직

```java
// 제목이 없거나 일반적인 제목인 경우
boolean needMetadataFetch = (title == null || title.isEmpty() || isGenericTitle(title));

if (needMetadataFetch && url != null) {
    String[] metadata = fetchMetadataFromUrl(url);
    if (metadata[0] != null && !metadata[0].isEmpty()) {
        article.setTitle(metadata[0]);  // 원본 기사 제목 업데이트
        title = metadata[0];
    }
    if (metadata[1] != null && !metadata[1].isEmpty() && content.isEmpty()) {
        article.setContent(metadata[1]);  // 원본 기사 본문 업데이트
        content = metadata[1];
    }
}
```

#### 3. AI 프롬프트 구조

```java
private static final String SUMMARY_PROMPT = """
    다음 기사를 분석하여 JSON 형식으로 응답해주세요.

    기사 제목: %s
    기사 내용: %s

    응답 형식:
    {
        "titleKo": "한글 제목 번역",
        "summary": "3-5문장 한국어 요약",
        "relevanceScore": 0.0-1.0,
        "category": "LLM|COMPUTER_VISION|NLP|...",
        "importance": "HIGH|MEDIUM|LOW"
    }

    필수 주의사항:
    - 외부 URL 접근 없이 제공된 제목과 내용만 사용
    - titleKo는 원문 제목을 자연스러운 한국어로 번역
    - summary는 한국어로 작성
    - 반드시 JSON 형식으로만 응답
    """;
```

#### 4. AI Provider 설정

**로컬 환경** (Claude CLI):
```yaml
ai:
  provider: claude-cli
  claude-cli:
    timeout: 180
```

**Railway 환경** (Claude API):
```yaml
ai:
  provider: claude
  claude:
    api-key: ${CLAUDE_API_KEY}
    model: claude-3-haiku-20240307
```

### API 엔드포인트

#### 단일 기사 분석
```http
POST /api/articles/{id}/analyze?force=false
```
- `force=true`: 이미 분석된 기사도 재분석

#### 배치 분석
```http
POST /api/articles/analyze-batch?limit=10
```
- 미분석 기사 일괄 처리
- API rate limit 방지를 위한 1초 딜레이

#### 중요도별 필터링
```http
GET /api/articles/importance/{importance}?page=0&size=20
```
- `importance`: HIGH, MEDIUM, LOW

### 프론트엔드 통합

#### 중요도 필터 UI (Articles.tsx)

```tsx
const [importanceFilter, setImportanceFilter] = useState<ArticleImportance | 'ALL'>('ALL');

const { data } = useQuery({
    queryKey: ['articles', page, importanceFilter],
    queryFn: () => {
        if (importanceFilter !== 'ALL') {
            return getArticlesByImportance(importanceFilter, page, 20);
        }
        return getArticles(page, 20);
    },
});
```

#### 필터 버튼 스타일
```tsx
{(['ALL', 'HIGH', 'MEDIUM', 'LOW'] as const).map((level) => (
    <button
        className={`px-3 py-1.5 text-sm rounded-lg ${
            importanceFilter === level
                ? level === 'HIGH' ? 'bg-red-600 text-white'
                : level === 'MEDIUM' ? 'bg-yellow-500 text-white'
                : level === 'LOW' ? 'bg-gray-600 text-white'
                : 'bg-blue-600 text-white'
                : 'bg-gray-100 text-gray-700'
        }`}
    >
        {level === 'ALL' ? '전체' : level === 'HIGH' ? '높음' : level === 'MEDIUM' ? '보통' : '낮음'}
    </button>
))}
```

### 에러 처리

#### 메타데이터 추출 실패
```java
try {
    // HTTP 요청 및 파싱
} catch (Exception e) {
    log.warn("URL 메타데이터 가져오기 실패: {} - {}", urlStr, e.getMessage());
    return new String[2];  // 빈 배열 반환
}
```

#### AI 분석 실패
```java
try {
    String response = callClaudeCli(prompt);
    // JSON 파싱 및 저장
} catch (Exception e) {
    log.error("AI 요약 생성 실패: {} - {}", article.getId(), e.getMessage());
    // 실패해도 다음 기사 계속 처리
}
```

### 성능 최적화

1. **본문 길이 제한**: 3000자 초과 시 truncate (토큰 절약)
2. **HEAD 태그 조기 종료**: `</head>` 발견 시 HTML 파싱 중단
3. **배치 처리**: 한 번에 여러 기사 분석 (rate limit 고려)
4. **캐싱**: 분석 완료 플래그(`isSummarized`)로 중복 분석 방지

### 관련 파일

| 파일 | 설명 |
|------|------|
| `AiSummaryService.java` | AI 분석 핵심 로직 |
| `NewsArticleService.java` | 기사 CRUD 및 업데이트 |
| `NewsArticleController.java` | REST API 엔드포인트 |
| `Articles.tsx` | 프론트엔드 기사 목록 및 필터 |
| `api/index.ts` | API 클라이언트 함수 |
| `types/index.ts` | TypeScript 타입 정의 |
