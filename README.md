# AI Insight - AI 뉴스 수집 에이전트

AI 최신 동향을 자동으로 수집, 요약, 제공하는 웹 애플리케이션입니다.

## 기술 스택

### Backend
- Java 17
- Spring Boot 3.2
- Spring Data JPA
- PostgreSQL / H2 (개발용)
- Jsoup (크롤링)
- Quartz (스케줄링)

### Frontend
- React 18 + TypeScript
- Vite
- Tailwind CSS 4
- React Query
- React Router

## 프로젝트 구조

```
AIInsight/
├── src/main/java/com/aiinsight/
│   ├── config/          # 설정 클래스
│   ├── controller/      # REST API 컨트롤러
│   ├── crawler/         # 크롤링 엔진
│   ├── domain/          # 엔티티 및 리포지토리
│   │   ├── article/     # 뉴스 기사
│   │   └── crawl/       # 크롤링 타겟/이력
│   ├── dto/             # DTO 클래스
│   ├── scheduler/       # 스케줄러
│   └── service/         # 비즈니스 로직
├── src/main/resources/
│   └── application.yml  # 설정 파일
└── frontend/            # React 프론트엔드
    ├── src/
    │   ├── api/         # API 클라이언트
    │   ├── components/  # 공통 컴포넌트
    │   ├── pages/       # 페이지 컴포넌트
    │   └── types/       # TypeScript 타입
    └── package.json
```

## 시작하기

### 백엔드 실행

```bash
# 빌드
./gradlew build

# 실행 (H2 인메모리 DB 사용)
./gradlew bootRun
```

백엔드 서버: http://localhost:8080
Swagger UI: http://localhost:8080/swagger-ui.html
H2 Console: http://localhost:8080/h2-console

### 프론트엔드 실행

```bash
cd frontend
npm install
npm run dev
```

프론트엔드: http://localhost:5173

## 환경 변수

### AI 요약 설정 (선택)
```bash
export OPENAI_API_KEY=your-openai-api-key
# 또는
export CLAUDE_API_KEY=your-claude-api-key
```

### 프로덕션 DB 설정
```bash
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=aiinsight
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
```

## 주요 기능

### 크롤링 타겟 관리
- 사이트 URL, CSS 선택자, 크롤링 주기 설정
- 활성화/비활성화 토글
- 수동 크롤링 실행

### 뉴스 기사 관리
- 수집된 기사 목록/상세 조회
- 카테고리별 필터링
- 키워드 검색
- 신규 기사 표시

### AI 요약
- OpenAI 또는 Claude API 연동
- 자동 카테고리 분류
- 관련성 점수 부여
- 중요도 판단

### 대시보드
- 실시간 통계
- 카테고리 분포
- 최근 크롤링 현황

## API 엔드포인트

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | /api/dashboard/stats | 대시보드 통계 |
| GET | /api/crawl-targets | 타겟 목록 |
| POST | /api/crawl-targets | 타겟 생성 |
| PUT | /api/crawl-targets/{id} | 타겟 수정 |
| DELETE | /api/crawl-targets/{id} | 타겟 삭제 |
| POST | /api/crawl/execute/{id} | 크롤링 실행 |
| GET | /api/articles | 기사 목록 |
| GET | /api/articles/{id} | 기사 상세 |
| GET | /api/articles/search | 기사 검색 |

## 선택자 설정 예시

```json
{
  "articleItemSelector": ".news-item",
  "titleSelector": "h2.title a",
  "linkSelector": "h2.title a",
  "contentSelector": ".content",
  "dateSelector": ".date",
  "dateFormat": "yyyy-MM-dd",
  "thumbnailSelector": "img.thumbnail",
  "pagination": {
    "enabled": true,
    "pageParamName": "page",
    "maxPages": 5
  }
}
```

## Cron 표현식 예시

| 표현식 | 설명 |
|--------|------|
| `0 0 * * * *` | 매시 정각 |
| `0 */30 * * * *` | 30분마다 |
| `0 0 */2 * * *` | 2시간마다 |
| `0 0 9 * * *` | 매일 오전 9시 |
