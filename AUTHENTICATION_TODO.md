# 인증 기능 구현 TODO

## 현재 상태 (2025-11-26)

### ✅ 완료된 작업
1. **의존성 추가** (`build.gradle`)
   - Spring Security
   - OAuth2 Client
   - JWT (JJWT 0.12.3)

2. **User 엔티티 생성** (`com.aiinsight.domain.user.User`)
   - 이메일 기반 사용자 식별
   - OAuth2 확장 가능 구조 (naverId 필드 포함)
   - UserRole (USER, ADMIN)

3. **UserRepository 생성**
   - 이메일 조회
   - naverId 조회 (나중에 OAuth2용)

4. **SecurityConfig 기본 구조**
   - 현재: 모든 요청 허용 (개발 단계)
   - OAuth2 추가 시 설정 변경 필요

---

## 🚧 향후 구현 필요 사항

### Phase 1: 네이버 OAuth2 인증 (우선순위: 높음)

#### 1. OAuth2 설정 (`application-*.yml`)
```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          naver:
            client-id: PXeI2aW8McLjWj6YDLEV
            client-secret: 5b9c4Ud5OP
            redirect-uri: "{baseUrl}/login/oauth2/code/naver"
            authorization-grant-type: authorization_code
            scope: name, email, profile_image
            client-name: Naver

        provider:
          naver:
            authorization-uri: https://nid.naver.com/oauth2.0/authorize
            token-uri: https://nid.naver.com/oauth2.0/token
            user-info-uri: https://openapi.naver.com/v1/nid/me
            user-name-attribute: response
```

#### 2. Custom OAuth2UserService 구현
**파일**: `com.aiinsight.security.OAuth2UserServiceImpl`
```java
@Service
public class OAuth2UserServiceImpl extends DefaultOAuth2UserService {
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        // 1. 네이버에서 사용자 정보 가져오기
        // 2. User 엔티티 생성 또는 업데이트
        // 3. naverId, email, name, profileImage 저장
        // 4. 마지막 로그인 시간 업데이트
    }
}
```

#### 3. JWT Token 유틸리티
**파일**: `com.aiinsight.security.JwtTokenProvider`
- 토큰 생성 (Access Token: 1시간, Refresh Token: 7일)
- 토큰 검증
- 사용자 정보 추출

#### 4. JWT Authentication Filter
**파일**: `com.aiinsight.security.JwtAuthenticationFilter`
- 요청 헤더에서 토큰 추출
- 토큰 검증 후 SecurityContext 설정

#### 5. AuthController
**파일**: `com.aiinsight.api.auth.AuthController`
```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    // GET /api/auth/me - 현재 사용자 정보
    // POST /api/auth/refresh - 토큰 갱신
    // POST /api/auth/logout - 로그아웃
}
```

#### 6. OAuth2 Success Handler
**파일**: `com.aiinsight.security.OAuth2SuccessHandler`
- 로그인 성공 시 JWT 토큰 생성
- 프론트엔드로 리다이렉트 (토큰 포함)

---

### Phase 2: 기존 엔티티 유저 연동

#### 1. NewsArticle 엔티티 수정
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id")
private User user;  // 기사를 크롤링한 사용자
```

#### 2. CrawlTarget 엔티티 수정
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id", nullable = false)
private User user;  // 타겟을 생성한 사용자
```

#### 3. CrawlHistory 엔티티 수정
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id")
private User user;  // 크롤링을 실행한 사용자
```

#### 4. Repository 쿼리 수정
- `findByUserId()` 메서드 추가
- 모든 조회 메서드에 사용자 필터링

#### 5. Controller 수정
- `@AuthenticationPrincipal` 또는 `SecurityContextHolder`에서 현재 사용자 가져오기
- 생성/수정 시 자동으로 userId 설정

---

### Phase 3: 프론트엔드 구현

#### 1. 랜딩 페이지 (`frontend/src/pages/Landing.tsx`)
- 네이버 로그인 버튼 (디자인 깔끔하게)
- AIInsight 소개
- 주요 기능 설명

#### 2. 인증 컨텍스트 (`frontend/src/contexts/AuthContext.tsx`)
```typescript
interface AuthContextType {
  user: User | null;
  loading: boolean;
  login: () => void;
  logout: () => void;
}
```

#### 3. Protected Routes
- 로그인 필요한 페이지 보호
- 미인증 사용자는 랜딩 페이지로 리다이렉트

#### 4. API 클라이언트 수정
- JWT 토큰을 모든 요청에 포함
- 토큰 만료 시 자동 갱신

---

### Phase 4: 마이그레이션 및 테스트

#### 1. 데이터베이스 마이그레이션
- 기존 데이터에 기본 사용자 할당
- userId NOT NULL 제약 조건 추가

#### 2. 통합 테스트
- OAuth2 로그인 흐름
- JWT 토큰 생성/검증
- 사용자별 데이터 필터링

#### 3. 프로덕션 배포
- Railway 환경변수 설정 (Client ID, Secret, JWT Secret)
- 콜백 URL 설정 확인

---

## 📝 참고 사항

### 네이버 OAuth2 콜백 URL
- **개발**: `http://localhost:8080/login/oauth2/code/naver`
- **운영**: `https://aiinsight-production.up.railway.app/login/oauth2/code/naver`

### JWT Secret 생성 (운영 환경)
```bash
openssl rand -base64 32
```

### 환경변수 설정 필요
```properties
# application-production.yml에 추가
jwt:
  secret: ${JWT_SECRET}
  access-token-validity: 3600000  # 1시간
  refresh-token-validity: 604800000  # 7일
```

---

## 🔗 관련 파일

### 백엔드
- [x] `src/main/java/com/aiinsight/domain/user/User.java`
- [x] `src/main/java/com/aiinsight/domain/user/UserRole.java`
- [x] `src/main/java/com/aiinsight/domain/user/UserRepository.java`
- [x] `src/main/java/com/aiinsight/config/SecurityConfig.java`
- [ ] `src/main/java/com/aiinsight/security/OAuth2UserServiceImpl.java`
- [ ] `src/main/java/com/aiinsight/security/JwtTokenProvider.java`
- [ ] `src/main/java/com/aiinsight/security/JwtAuthenticationFilter.java`
- [ ] `src/main/java/com/aiinsight/security/OAuth2SuccessHandler.java`
- [ ] `src/main/java/com/aiinsight/api/auth/AuthController.java`

### 프론트엔드
- [ ] `frontend/src/pages/Landing.tsx`
- [ ] `frontend/src/contexts/AuthContext.tsx`
- [ ] `frontend/src/components/ProtectedRoute.tsx`
- [ ] `frontend/src/api/auth.ts`

---

## 📌 현재 작동 상태

✅ **서버 정상 작동**
- Spring Security 활성화됨 (모든 요청 허용)
- User 테이블 생성됨
- 기존 기능 정상 작동 (48개 기사 확인)

🚧 **다음 단계**
1. 네이버 OAuth2 설정 추가
2. JWT 토큰 시스템 구현
3. 프론트엔드 랜딩 페이지 구현
