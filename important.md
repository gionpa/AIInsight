# AIInsight 프로젝트 중요 설정 및 개발 노트

## 프로젝트 개요
- **프로젝트명**: AIInsight - AI 뉴스 스크래핑 및 분석 시스템
- **프로덕션 URL**: https://aiinsight-production.up.railway.app
- **기술 스택**: Spring Boot 3.2, Java 21, React 18, TypeScript, PostgreSQL, Redis

---

## Railway 프로덕션 환경

### 데이터베이스 접속 정보
```
Host: yamanote.proxy.rlwy.net
Port: 51273
Database: railway
Username: postgres
Password: yOPQIglOJVBrJtUlCMVhVqLQLhEFLwXg
```

### Redis 접속 정보
```
Host: interchange.proxy.rlwy.net
Port: 19189
Password: EHndhkZGTobgKYkUrgWIhevIpYLQivNy
```

### 필수 환경 변수 (Railway Variables)
```
COOKIE_SECURE=true
FRONTEND_URL=https://aiinsight-production.up.railway.app
JWT_SECRET=<your-jwt-secret>
NAVER_CLIENT_ID=PXeI2aW8McLjWj6YDLEV
NAVER_CLIENT_SECRET=<your-naver-client-secret>
SPRING_PROFILES_ACTIVE=railway
```

---

## 해결된 주요 이슈들

### 1. OAuth2 로그인 500 에러 (2025-12-02)

**문제**: 네이버 OAuth2 로그인 콜백(`/login/oauth2/code/naver`)에서 500 Internal Server Error 발생

**원인**: PostgreSQL `users` 테이블의 `email` 컬럼에 NOT NULL 제약 조건이 있었으나, 네이버에서 일부 사용자는 이메일을 제공하지 않음

**해결책**:
```sql
ALTER TABLE users ALTER COLUMN email DROP NOT NULL;
```

**관련 파일**:
- `src/main/java/com/aiinsight/domain/user/User.java` - email 컬럼 정의
- `src/main/java/com/aiinsight/security/CustomOAuth2UserService.java` - OAuth2 사용자 처리

---

### 2. OAuth2 redirect_uri HTTP vs HTTPS 불일치

**문제**: Railway 프록시 뒤에서 redirect_uri가 HTTP로 생성되어 네이버 OAuth2에서 거부됨

**해결책**: `application-railway.yml`에 forward-headers-strategy 설정 추가
```yaml
server:
  forward-headers-strategy: framework
```

**관련 파일**:
- `src/main/resources/application-railway.yml`
- `src/main/resources/application.yml`

---

### 3. 정적 리소스 403 Forbidden 에러

**문제**: 프로덕션에서 `/assets/**`, `/*.js`, `/*.css` 등 정적 리소스 접근 시 403 에러

**해결책**: SecurityConfig에 정적 리소스 경로 permitAll 추가 및 CORS 설정 강화

**관련 파일**:
- `src/main/java/com/aiinsight/security/SecurityConfig.java`

```java
.requestMatchers(
    "/assets/**",
    "/vite.svg",
    "/*.js",
    "/*.css",
    "/*.ico",
    "/*.png",
    "/*.svg"
).permitAll()
```

---

## 주요 설정 파일

### application-railway.yml 핵심 설정
```yaml
server:
  port: ${PORT:8080}
  forward-headers-strategy: framework  # HTTPS 프록시 지원

app:
  frontend-url: https://aiinsight-production.up.railway.app
  cookie-secure: true  # HTTPS에서 쿠키 보안

management:
  health:
    db:
      enabled: false  # DB health check 비활성화 (빠른 부팅)
    redis:
      enabled: false  # Redis health check 비활성화
```

### SecurityConfig 핵심 설정
- CORS: localhost:5173, localhost:3000, aiinsight-production.up.railway.app 허용
- CSRF: 비활성화 (API 서버)
- 세션: STATELESS (JWT 기반)

---

## 네이버 OAuth2 설정

### 네이버 개발자 센터 설정
- **Client ID**: PXeI2aW8McLjWj6YDLEV
- **Callback URL**: https://aiinsight-production.up.railway.app/login/oauth2/code/naver
- **필수 권한**: 이름, 이메일, 프로필 이미지

### Spring Security OAuth2 설정
```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          naver:
            client-id: ${NAVER_CLIENT_ID}
            client-secret: ${NAVER_CLIENT_SECRET}
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
            scope: name,email,profile_image
```

---

## 데이터베이스 스키마 주의사항

### users 테이블
- `email`: nullable (네이버에서 이메일 미제공 가능)
- `naver_id`: nullable (네이버 사용자 ID)
- `name`: NOT NULL
- `role`: NOT NULL (기본값: USER)

### 스키마 변경 시 주의
- Railway PostgreSQL에서는 `ddl-auto: update` 사용
- 컬럼 제약 조건 변경은 직접 SQL 실행 필요

---

## 로컬 개발 환경

### 실행 방법
```bash
# 백엔드 (포트 8080)
./gradlew bootRun

# 프론트엔드 (포트 5173)
cd frontend && npm run dev
```

### 로컬 데이터베이스
- H2 인메모리 DB 사용 (`application.yml` 기본 프로파일)

---

## 트러블슈팅 가이드

### Railway 로그 확인
```bash
railway logs
```

### PostgreSQL 직접 접속 (Python)
```python
import psycopg2
conn = psycopg2.connect(
    host="yamanote.proxy.rlwy.net",
    port=51273,
    database="railway",
    user="postgres",
    password="yOPQIglOJVBrJtUlCMVhVqLQLhEFLwXg"
)
```

### OAuth2 디버깅
- `CustomOAuth2UserService.java`에 상세 로그 추가됨
- Railway 로그에서 "OAuth2 loadUser" 관련 로그 확인

---

## 향후 작업 시 참고사항

1. **새 컬럼 추가 시**: JPA 엔티티와 DB 스키마 동기화 확인
2. **OAuth2 수정 시**: redirect_uri HTTPS 유지 확인
3. **보안 설정 변경 시**: 정적 리소스 접근 권한 확인
4. **환경 변수 추가 시**: Railway Variables에 등록

---

*마지막 업데이트: 2025-12-02*
