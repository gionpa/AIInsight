# Railway 배포 문제 해결 가이드

## 현재 문제: PostgreSQL 연결 실패

### 증상
```
SQLState: 08001
The connection attempt failed.
connection timeout expired
```

### 원인

Railway PostgreSQL은 **Private Network**로 구성되어 있을 수 있습니다:
- Railway 외부(로컬 개발 환경)에서는 접근 불가
- Railway 내부(배포된 서비스)에서만 접근 가능

### 해결 방법

#### 1. Railway Dashboard에서 최신 연결 정보 확인

1. **Railway Dashboard** 접속: https://railway.app
2. 프로젝트 선택
3. **PostgreSQL 서비스** 클릭
4. **Variables** 또는 **Connect** 탭에서 연결 정보 확인:
   ```
   PGHOST=xxxxxx.railway.app
   PGPORT=xxxxx
   PGDATABASE=railway
   PGUSER=postgres
   PGPASSWORD=xxxxx
   DATABASE_URL=postgresql://...
   ```

#### 2. Railway 환경변수 설정 확인

1. Railway Dashboard → 애플리케이션 서비스 선택
2. **Variables** 탭 클릭
3. PostgreSQL 환경변수가 자동으로 주입되었는지 확인:
   - `DATABASE_URL` (권장)
   - 또는 `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD`

#### 3. 서비스 연결 확인

Railway에서 PostgreSQL과 애플리케이션이 연결되어 있는지 확인:

1. Railway Dashboard → 프로젝트 선택
2. 그래프 뷰에서 PostgreSQL과 애플리케이션 서비스 사이에 **화살표(연결선)** 확인
3. 연결되어 있지 않다면:
   - PostgreSQL 서비스 클릭 → **Connect** 버튼
   - 애플리케이션 서비스 선택하여 연결

#### 4. Private Networking 확인

Railway PostgreSQL이 Private Network로 구성된 경우:
- ✅ Railway 내부: 자동으로 접근 가능
- ❌ 로컬 개발 환경: 접근 불가
- ❌ 외부 IP: 접근 불가

**Public Network 활성화** (선택사항):
1. PostgreSQL 서비스 → **Settings** 탭
2. **Networking** 섹션
3. **Public Networking** 활성화

⚠️ **보안 경고**: Public Networking은 보안 위험이 있으므로 프로덕션에서는 권장하지 않습니다.

## Railway 배포 전용 설정

### Option 1: 환경변수 자동 주입 (권장)

Railway는 연결된 서비스의 환경변수를 자동으로 주입합니다.

**application-railway.yml 수정**:
```yaml
spring:
  datasource:
    # Railway가 자동으로 주입하는 DATABASE_URL 사용
    url: ${DATABASE_URL}
    driver-class-name: org.postgresql.Driver
    username: ${PGUSER}
    password: ${PGPASSWORD}
```

**장점**: 연결 정보가 변경되어도 자동으로 업데이트됨

### Option 2: 하드코딩 제거 (현재 문제)

현재 `application-railway.yml`에 하드코딩된 Railway 연결 정보는:
- 오래된 정보일 수 있음
- Private Network 주소일 수 있음
- 외부에서 접근 불가할 수 있음

**해결**: Railway 환경변수만 사용하도록 수정

```yaml
spring:
  datasource:
    url: ${DATABASE_URL}
    username: ${PGUSER}
    password: ${PGPASSWORD}
```

## 로컬 개발 환경 설정

Railway PostgreSQL을 로컬에서 사용할 수 없는 경우:

### Option 1: 로컬 H2 Database (권장)

**application.yml** (기본 프로파일):
```yaml
spring:
  datasource:
    url: jdbc:h2:file:./data/aiinsight
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: create-drop
  h2:
    console:
      enabled: true
```

**실행**:
```bash
# railway 프로파일 없이 실행 → H2 사용
./gradlew bootRun
```

### Option 2: 로컬 PostgreSQL

Docker로 로컬 PostgreSQL 실행:

```bash
docker run -d \
  --name aiinsight-postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=aiinsight \
  -p 5432:5432 \
  postgres:15
```

**application-local.yml**:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/aiinsight
    username: postgres
    password: postgres
```

**실행**:
```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

## Railway 배포 체크리스트

### 배포 전
- [ ] PostgreSQL 서비스 추가 및 연결 확인
- [ ] Redis 서비스 추가 및 연결 확인
- [ ] 환경변수 자동 주입 확인 (`DATABASE_URL` 등)
- [ ] `application-railway.yml`에서 하드코딩 제거
- [ ] 데이터베이스 스키마 초기화 (Railway Shell에서 실행)

### 배포 후
- [ ] Railway 로그에서 애플리케이션 시작 확인
- [ ] PostgreSQL 연결 성공 로그 확인
- [ ] Health check 엔드포인트 확인 (`/actuator/health`)
- [ ] 프론트엔드 접속 테스트
- [ ] API 테스트 (`/api`)

## Railway Shell로 데이터베이스 초기화

Railway 내부에서만 PostgreSQL 접근이 가능한 경우:

1. Railway Dashboard → PostgreSQL 서비스
2. **Shell** 탭 클릭
3. PostgreSQL CLI 실행:
```bash
# 테이블 생성 SQL 복사 후 실행
psql $DATABASE_URL < schema.sql
```

또는 애플리케이션 서비스에서:
```bash
# Railway Shell에서
curl -X POST http://localhost:8080/api/admin/init-db
```

## 추가 문제 해결

### 1. Redis 연결 오류

**증상**: `Could not connect to Redis`

**해결**:
- Railway Dashboard에서 Redis 연결 확인
- `REDIS_URL` 환경변수 확인
- Redis 서비스가 실행 중인지 확인

### 2. 빌드 실패

**증상**: `npm command not found`

**해결**:
- `nixpacks.toml`에 Node.js 런타임 추가 확인
- Frontend 빌드가 성공했는지 로그 확인

### 3. Health check 실패

**증상**: Railway가 서비스를 unhealthy로 표시

**해결**:
- `/actuator/health` 엔드포인트가 200 OK 반환하는지 확인
- `railway.toml`의 `healthcheckTimeout` 늘리기
- 애플리케이션 시작 시간이 오래 걸리는 경우 대기 시간 증가

## 권장 사항

### 1. 환경변수 의존
하드코딩된 연결 정보 대신 Railway 환경변수에만 의존:
```yaml
url: ${DATABASE_URL}
host: ${REDIS_HOST}
```

### 2. Railway Private Network 사용
외부 노출 없이 안전한 내부 통신:
- PostgreSQL: Private Network만
- Redis: Private Network만
- 애플리케이션: Public Network (사용자 접근용)

### 3. 로컬 개발과 프로덕션 분리
- 로컬: H2 또는 Docker PostgreSQL
- Railway: Railway PostgreSQL
- 프로파일로 분리 관리

### 4. 데이터베이스 초기화
- Railway Shell에서 수동 실행
- 또는 `ddl-auto: update` 사용 (주의 필요)
- Migration 도구 사용 (Flyway, Liquibase)

## 연락처 및 지원

- **Railway 문서**: https://docs.railway.app
- **Railway Discord**: https://discord.gg/railway
- **PostgreSQL 문서**: https://www.postgresql.org/docs/
