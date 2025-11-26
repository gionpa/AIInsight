# 환경 설정 가이드

## 환경 구분

AIInsight 프로젝트는 **local**과 **production** 두 가지 환경을 지원합니다.

## 1. Local 환경 (기본값)

### 사용 인프라
- **PostgreSQL**: Podman (localhost:5432)
- **Redis**: Podman (localhost:6379)
- **포트**: 8080

### 실행 방법

```bash
# 방법 1: 기본 실행 (자동으로 local 프로파일 사용)
./gradlew bootRun

# 방법 2: 명시적으로 local 프로파일 지정
./gradlew bootRun --args='--spring.profiles.active=local'

# 방법 3: 환경변수로 지정
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```

### Podman 데이터베이스 시작

```bash
# PostgreSQL 시작
podman start aiinsight-postgres
# 또는
./start-db.sh

# Redis 시작
podman-compose up -d
```

### 설정 파일
- `application.yml`: 기본 설정 (profiles.active: local)
- `application-local.yml`: Local 환경 전용 설정

### 특징
- SQL 로그 출력 (`show-sql: true`)
- Hibernate DDL auto-update
- Swagger UI 활성화 (http://localhost:8080/swagger-ui.html)
- 디버그 로그 레벨

## 2. Production 환경 (Railway)

### 사용 인프라
- **PostgreSQL**: Railway PostgreSQL (yamanote.proxy.rlwy.net:51273)
- **Redis**: Railway Redis (interchange.proxy.rlwy.net:19189)
- **포트**: Railway 할당 포트 ($PORT)

### 자동 배포
Railway는 GitHub push 시 자동으로 배포됩니다:

```bash
git push origin main
```

### 설정 방법

#### nixpacks.toml
```toml
[start]
cmd = "java -Dserver.port=$PORT -Dspring.profiles.active=production -jar build/libs/aiinsight-0.0.1-SNAPSHOT.jar"
```

#### Railway 환경변수
Railway Dashboard에서 자동으로 설정됨:
- `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD`
- `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`
- `PORT` (Railway 할당)

### 설정 파일
- `application-production.yml`: Production 환경 전용 설정
- `application-railway.yml`: 하위 호환성 (production과 동일)

### 특징
- SQL 로그 비활성화 (`show-sql: false`)
- Swagger UI 비활성화 (보안)
- HTTP/2 및 압축 활성화
- INFO 로그 레벨

## 환경별 차이점 요약

| 항목 | Local | Production |
|-----|-------|------------|
| **PostgreSQL** | localhost:5432 | Railway PostgreSQL |
| **Redis** | localhost:6379 | Railway Redis |
| **SQL 로그** | ✅ 활성화 | ❌ 비활성화 |
| **Swagger UI** | ✅ 활성화 | ❌ 비활성화 |
| **로그 레벨** | DEBUG | INFO |
| **DDL Auto** | update | update |
| **HTTP/2** | ❌ | ✅ 활성화 |
| **압축** | ❌ | ✅ 활성화 |

## 프로파일 전환

### IDE (IntelliJ IDEA)
1. Run/Debug Configurations
2. Environment variables: `SPRING_PROFILES_ACTIVE=local` 또는 `production`
3. 또는 VM options: `-Dspring.profiles.active=local`

### Gradle
```bash
# Local
./gradlew bootRun

# Production (로컬에서 테스트)
SPRING_PROFILES_ACTIVE=production ./gradlew bootRun
```

### JAR 실행
```bash
# Local
java -jar build/libs/aiinsight-0.0.1-SNAPSHOT.jar

# Production
java -Dspring.profiles.active=production -jar build/libs/aiinsight-0.0.1-SNAPSHOT.jar
```

## 문제 해결

### Local 환경에서 PostgreSQL 연결 실패
```bash
# Podman 컨테이너 상태 확인
podman ps

# 컨테이너 시작
./start-db.sh

# 연결 테스트
psql -h localhost -p 5432 -U aiinsight -d aiinsight
```

### Production 환경에서 데이터 없음
1. Railway Dashboard에서 환경변수 확인
2. Railway 로그에서 PostgreSQL 연결 확인
3. Railway Shell에서 데이터베이스 확인:
```bash
psql $DATABASE_URL
\dt  # 테이블 목록
SELECT COUNT(*) FROM news_article;
```

## 추가 정보

- **Frontend 개발**: `cd frontend && npm run dev` (Vite 개발 서버 사용)
- **Frontend 빌드**: Railway nixpacks가 자동으로 빌드
- **데이터 마이그레이션**: `python3 clean_and_migrate_correct.py`
