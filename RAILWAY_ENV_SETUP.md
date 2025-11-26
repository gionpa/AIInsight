# Railway 환경변수 설정 가이드

## 필수 환경변수 설정

Railway Dashboard → 프로젝트 선택 → Variables 탭에서 다음 환경변수를 설정해야 합니다.

### 1. PostgreSQL 환경변수

Railway에서 PostgreSQL을 추가하면 자동으로 생성되지만, 수동으로 확인 및 설정:

```bash
# PostgreSQL 연결 정보 (Railway PostgreSQL 서비스에서 자동 생성)
PGHOST=containers-us-west-110.railway.app
PGPORT=7539
PGDATABASE=railway
PGUSER=postgres
PGPASSWORD=yOPQIglOJVBrJtUlCMVhVqLQLhEFLwXg

# 또는 전체 URL (선택사항, 위 개별 설정이 우선)
DATABASE_URL=postgresql://postgres:yOPQIglOJVBrJtUlCMVhVqLQLhEFLwXg@containers-us-west-110.railway.app:7539/railway
```

### 2. Redis 환경변수

Railway에서 Redis를 추가하면 자동으로 생성:

```bash
# Redis 연결 정보 (Railway Redis 서비스에서 자동 생성)
REDIS_HOST=interchange.proxy.rlwy.net
REDIS_PORT=19189
REDIS_PASSWORD=EHndhkZGTobgKYkUrgWIhevIpYLQivNy

# 또는 전체 URL (선택사항, 개별 설정이 fallback)
REDIS_URL=redis://default:EHndhkZGTobgKYkUrgWIhevIpYLQivNy@interchange.proxy.rlwy.net:19189
```

### 3. 애플리케이션 환경변수

```bash
# Spring Boot 프로파일
SPRING_PROFILES_ACTIVE=railway

# 서버 포트 (Railway가 자동 설정, 변경 불필요)
PORT=8080

# Swagger UI (프로덕션에서는 비활성화 권장)
SWAGGER_ENABLED=false
```

### 4. AI API 키 (선택사항)

Claude CLI를 사용하는 경우 API 키 불필요. OpenAI나 Claude API를 직접 사용하려면:

```bash
# OpenAI API (선택)
OPENAI_API_KEY=your-openai-api-key

# Claude API (선택)
CLAUDE_API_KEY=your-claude-api-key
```

## 환경변수 확인 방법

### Railway Dashboard에서 확인

1. Railway Dashboard 접속
2. 프로젝트 선택
3. **Variables** 탭 클릭
4. PostgreSQL과 Redis 서비스의 환경변수 확인

### Railway CLI로 확인 (선택)

```bash
# Railway CLI 설치
npm i -g @railway/cli

# 로그인
railway login

# 환경변수 조회
railway variables
```

## 환경변수 Fallback 전략

`application-railway.yml`은 다음과 같은 fallback 전략을 사용합니다:

### PostgreSQL
```yaml
# 1순위: DATABASE_URL 환경변수
# 2순위: PGHOST, PGPORT, PGDATABASE 조합
# 3순위: 하드코딩된 기본값
url: ${DATABASE_URL:jdbc:postgresql://${PGHOST:containers-us-west-110.railway.app}:${PGPORT:7539}/${PGDATABASE:railway}}
username: ${PGUSER:postgres}
password: ${PGPASSWORD:yOPQIglOJVBrJtUlCMVhVqLQLhEFLwXg}
```

### Redis
```yaml
# 1순위: REDIS_URL 환경변수
# 2순위: REDIS_HOST, REDIS_PORT, REDIS_PASSWORD 조합
# 3순위: 하드코딩된 기본값
url: ${REDIS_URL:}
host: ${REDIS_HOST:interchange.proxy.rlwy.net}
port: ${REDIS_PORT:19189}
password: ${REDIS_PASSWORD:EHndhkZGTobgKYkUrgWIhevIpYLQivNy}
```

## 문제 해결

### 1. DATABASE_URL 오류

**증상**: `Driver org.postgresql.Driver claims to not accept jdbcUrl, ${DATABASE_URL}`

**원인**: 환경변수가 설정되지 않아 `${DATABASE_URL}` 문자열이 그대로 전달됨

**해결**:
1. Railway Dashboard → Variables에서 PostgreSQL 환경변수 확인
2. `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD` 모두 설정되었는지 확인
3. 설정이 없다면 PostgreSQL 서비스를 다시 연결하거나 수동으로 추가

### 2. Redis 연결 오류

**증상**: Redis 연결 실패

**해결**:
1. Railway Dashboard → Variables에서 Redis 환경변수 확인
2. `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` 확인
3. Redis 서비스가 실행 중인지 확인

### 3. Railway 서비스 연결

Railway에서 서비스를 연결하려면:

1. Railway Dashboard → 프로젝트 선택
2. **+ New** → **Database** 선택
3. PostgreSQL 또는 Redis 선택
4. 자동으로 환경변수가 생성됨
5. 애플리케이션 서비스를 다시 배포하여 환경변수 적용

## 보안 주의사항

⚠️ **중요**:
- 이 문서의 환경변수는 예시입니다
- **실제 프로덕션 환경에서는 Railway Dashboard에서 생성된 값을 사용하세요**
- 환경변수를 Git에 커밋하지 마세요 (`.gitignore`에 이미 추가됨)
- Railway Dashboard의 Variables에서만 관리하세요

## 배포 체크리스트

- [ ] PostgreSQL 서비스 추가 완료
- [ ] Redis 서비스 추가 완료
- [ ] 모든 필수 환경변수 설정 확인
- [ ] 데이터베이스 스키마 생성 완료 (`setup_railway_db.py` 실행)
- [ ] Railway 배포 성공
- [ ] Health check 정상 동작 확인 (`/actuator/health`)
- [ ] 프론트엔드 접속 테스트
- [ ] API 호출 테스트 (`/api`)
