# Railway 배포 가이드

AIInsight 애플리케이션을 Railway에 배포하는 방법을 설명합니다.

## 📋 사전 준비

### 1. Railway 계정 및 프로젝트 생성
- [Railway](https://railway.app/) 계정 가입
- 새 프로젝트 생성

### 2. 데이터베이스 설정
Railway 프로젝트에 다음 서비스를 추가해야 합니다:

#### PostgreSQL
- Railway Dashboard에서 `New` → `Database` → `Add PostgreSQL` 선택
- 자동으로 환경변수가 생성됩니다:
  - `DATABASE_URL`
  - `PGHOST`
  - `PGPORT`
  - `PGUSER`
  - `PGPASSWORD`
  - `PGDATABASE`

#### Redis
- Railway Dashboard에서 `New` → `Database` → `Add Redis` 선택
- 자동으로 환경변수가 생성됩니다:
  - `REDIS_URL` (전체 연결 URL, 우선순위)
  - `REDIS_HOST`
  - `REDIS_PORT`
  - `REDIS_PASSWORD`

**✅ 현재 상태**: Redis 생성 완료 및 연결 테스트 성공
- **버전**: Redis 8.2.1
- **연결 테스트**: 모든 작업(SET/GET/DELETE) 정상 동작 확인

## 🗄️ 데이터베이스 초기화

PostgreSQL 테이블을 생성하기 위해 제공된 스크립트를 실행합니다:

```bash
# Python 스크립트 실행
python3 setup_railway_db.py
```

또는 Railway CLI를 사용:

```bash
# Railway CLI 설치
npm i -g @railway/cli

# 로그인
railway login

# 프로젝트 연결
railway link

# PostgreSQL에 직접 연결하여 스키마 실행
railway run psql $DATABASE_URL < railway-schema.sql
```

## 🚀 배포 방법

### 방법 1: GitHub 연동 (권장)

1. **GitHub 저장소 연결**
   ```bash
   git remote add origin https://github.com/gionpa/AIInsight.git
   git push -u origin main
   ```

2. **Railway에서 GitHub 저장소 연결**
   - Railway Dashboard → `New` → `GitHub Repo` 선택
   - AIInsight 저장소 선택
   - 자동으로 빌드 및 배포 시작

3. **자동 배포 설정**
   - `main` 브랜치에 push하면 자동으로 배포됩니다
   - Pull Request마다 Preview 환경 생성 가능

### 방법 2: Railway CLI

```bash
# Railway CLI 설치
npm i -g @railway/cli

# 로그인
railway login

# 프로젝트 연결
railway link

# 배포
railway up
```

## ⚙️ 환경 변수 설정

Railway Dashboard에서 다음 환경변수를 설정해야 합니다:

### 필수 환경변수

```bash
# 데이터베이스 (PostgreSQL 서비스 추가 시 자동 생성)
DATABASE_URL=postgresql://postgres:password@host:port/database
PGHOST=yamanote.proxy.rlwy.net
PGPORT=51273
PGUSER=postgres
PGPASSWORD=your_password
PGDATABASE=railway

# Redis (Redis 서비스 추가 시 자동 생성)
REDIS_HOST=redis.railway.internal
REDIS_PORT=6379
REDIS_PASSWORD=your_redis_password

# 애플리케이션
PORT=8080
SPRING_PROFILES_ACTIVE=railway
```

### 선택 환경변수

```bash
# AI API (사용하는 경우)
OPENAI_API_KEY=sk-...
CLAUDE_API_KEY=sk-ant-...

# Swagger UI (프로덕션에서는 비활성화 권장)
SWAGGER_ENABLED=false
```

## 📦 빌드 설정

Railway는 자동으로 프로젝트를 감지하고 빌드합니다.

### railway.toml
프로젝트 루트의 `railway.toml` 파일에서 빌드 설정을 확인할 수 있습니다:

```toml
[build]
builder = "NIXPACKS"

[deploy]
startCommand = "java -Dserver.port=$PORT -Dspring.profiles.active=railway -jar build/libs/aiinsight-0.0.1-SNAPSHOT.jar"

[deploy.healthcheckPath]
path = "/actuator/health"
```

## 🔍 헬스체크

Railway는 `/actuator/health` 엔드포인트를 사용하여 애플리케이션의 상태를 모니터링합니다.

헬스체크 응답:
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP"
    },
    "redis": {
      "status": "UP"
    }
  }
}
```

## 🌐 도메인 설정

### Railway 제공 도메인
- Railway가 자동으로 `*.railway.app` 도메인을 제공합니다
- 예: `aiinsight-production.up.railway.app`

### 커스텀 도메인
1. Railway Dashboard → 서비스 선택 → `Settings` → `Domains`
2. `Custom Domain` 추가
3. DNS 설정에서 CNAME 레코드 추가
   ```
   CNAME your-domain.com → your-service.up.railway.app
   ```

## 📊 로그 확인

### Railway Dashboard
- 서비스 선택 → `Deployments` → 최신 배포 선택
- 실시간 로그 확인 가능

### Railway CLI
```bash
# 실시간 로그 확인
railway logs

# 특정 서비스 로그
railway logs --service aiinsight
```

## 🔧 트러블슈팅

### 1. 빌드 실패
**증상**: `BUILD FAILED` 에러
**해결**:
```bash
# 로컬에서 빌드 테스트
./gradlew clean build

# Gradle wrapper 실행 권한 확인
git update-index --chmod=+x gradlew
git commit -m "Fix gradlew permissions"
git push
```

### 2. 데이터베이스 연결 실패
**증상**: `Connection refused` 또는 `Connection timeout`
**해결**:
- Railway Dashboard에서 PostgreSQL 서비스가 실행 중인지 확인
- 환경변수 `DATABASE_URL`이 올바른지 확인
- PostgreSQL 내부 네트워크 주소 사용: `postgres.railway.internal`

### 3. Redis 연결 실패
**증상**: `Unable to connect to Redis`
**해결**:
- Railway Dashboard에서 Redis 서비스가 실행 중인지 확인
- 환경변수 `REDIS_HOST`, `REDIS_PORT` 확인
- Redis 내부 네트워크 주소 사용: `redis.railway.internal`

### 4. 애플리케이션 시작 실패
**증상**: Health check 실패
**해결**:
```bash
# 로그 확인
railway logs

# 일반적인 원인:
# - 환경변수 누락
# - 데이터베이스 스키마 미생성
# - 포트 설정 오류 (PORT 환경변수 확인)
```

### 5. Out of Memory
**증상**: `java.lang.OutOfMemoryError`
**해결**:
- Railway Dashboard → Settings → Resources
- 메모리 할당량 증가
- 또는 JVM 메모리 옵션 조정:
  ```bash
  JAVA_OPTS=-Xmx512m -Xms256m
  ```

## 🔐 보안 권장사항

### 1. 환경변수 보안
- API 키, 비밀번호는 절대 소스코드에 포함하지 마세요
- Railway의 환경변수 기능 사용

### 2. Swagger UI 비활성화
프로덕션 환경에서는 Swagger UI를 비활성화하세요:
```bash
SWAGGER_ENABLED=false
```

### 3. 데이터베이스 백업
- Railway는 자동 백업을 제공합니다
- 중요한 데이터는 정기적으로 별도 백업 권장

### 4. HTTPS 강제
Railway는 기본적으로 HTTPS를 제공하지만, 애플리케이션 레벨에서도 강제할 수 있습니다.

## 📈 모니터링

### Railway 기본 모니터링
- CPU, 메모리, 네트워크 사용량
- 배포 이력 및 상태
- 로그 수집 및 검색

### 추가 모니터링 (선택사항)
- Spring Boot Actuator 엔드포인트 활용
  - `/actuator/health` - 헬스체크
  - `/actuator/metrics` - 메트릭
  - `/actuator/info` - 애플리케이션 정보

## 🎯 성능 최적화

### 1. Redis 캐싱 활용
- 기사 목록 캐싱 (30분)
- 리포트 캐싱 (15분)
- 크롤 타겟 캐싱 (10분)

### 2. 데이터베이스 커넥션 풀
```yaml
hikari:
  maximum-pool-size: 10
  minimum-idle: 2
  connection-timeout: 30000
```

### 3. HTTP/2 활성화
```yaml
server:
  http2:
    enabled: true
  compression:
    enabled: true
```

## 📞 지원

### Railway 지원
- [Railway Discord](https://discord.gg/railway)
- [Railway 문서](https://docs.railway.app/)
- [Railway 커뮤니티](https://help.railway.app/)

### 프로젝트 관련
- GitHub Issues: https://github.com/gionpa/AIInsight/issues

## 📝 체크리스트

배포 전 확인사항:

- [ ] PostgreSQL 서비스 추가 및 스키마 생성
- [ ] Redis 서비스 추가
- [ ] 필수 환경변수 설정
- [ ] GitHub 저장소 연결 (또는 Railway CLI 설정)
- [ ] `railway.toml` 파일 확인
- [ ] 로컬에서 빌드 테스트 성공
- [ ] Health check 엔드포인트 동작 확인
- [ ] Swagger UI 비활성화 (프로덕션)
- [ ] 도메인 설정 (필요시)
- [ ] 모니터링 설정

배포 완료!
