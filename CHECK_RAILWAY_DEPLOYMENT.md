# Railway 배포 확인 체크리스트

## 1. Railway Dashboard 확인 사항

### 배포 상태
1. **Railway Dashboard** → 프로젝트 선택
2. **Deployments** 탭에서 최근 배포 상태 확인
   - ✅ Success (성공)
   - ❌ Failed (실패)
   - 🔄 Building (빌드 중)

### 배포 로그 확인
**Deployments** → 최신 배포 클릭 → **Logs** 확인:

#### Frontend 빌드 로그 확인 항목
```
✅ cd frontend && npm install
✅ cd frontend && npm run build
✅ frontend/dist 생성 확인
✅ Report.tsx 포함 확인
```

#### Backend 빌드 로그 확인 항목
```
✅ export RAILWAY_ENVIRONMENT=production
✅ ./gradlew clean build -x test
✅ copyFrontend 실행 (frontend/dist → static/)
✅ aiinsight-0.0.1-SNAPSHOT.jar 생성
```

#### 실행 로그 확인 항목
```
✅ Spring Boot 시작
✅ PostgreSQL 연결 성공 (yamanote.proxy.rlwy.net:51273)
✅ Redis 연결 성공 (interchange.proxy.rlwy.net:19189)
✅ Application started on port $PORT
```

## 2. 서비스 URL 확인

### Railway 도메인 찾기
1. Railway Dashboard → 서비스 선택
2. **Settings** → **Networking** 섹션
3. **Public Domain** 확인
   - 예: `https://aiinsight-production.up.railway.app`

## 3. API 테스트

배포된 URL로 다음 엔드포인트 테스트:

### Health Check
```bash
curl https://YOUR-DOMAIN.up.railway.app/actuator/health
```

**예상 응답**:
```json
{"status":"UP"}
```

### 크롤링 타겟 확인
```bash
curl https://YOUR-DOMAIN.up.railway.app/api/crawl-targets/all
```

**예상**: 13개의 크롤링 타겟 JSON 배열

### 기사 목록 확인
```bash
curl https://YOUR-DOMAIN.up.railway.app/api/articles?page=0&size=10
```

**예상**: 기사 목록 JSON (Railway PostgreSQL 데이터)

## 4. Frontend 확인

### 브라우저에서 접속
```
https://YOUR-DOMAIN.up.railway.app/
```

#### 확인 항목
- [ ] 대시보드 페이지 로드
- [ ] **리포트** 메뉴 표시 (왼쪽 사이드바)
- [ ] 기사 목록에 데이터 표시
- [ ] 날짜/시간 정보 표시

### 개발자 도구 확인 (F12)
1. **Console** 탭
   - ❌ 에러 메시지 확인
   - Network 요청 실패 확인

2. **Network** 탭
   - API 요청 URL 확인 (`/api/...`)
   - 응답 상태 코드 (200, 404, 500 등)
   - 응답 데이터 확인

## 5. 문제 진단

### 문제 1: 리포트 메뉴가 안 보임

**원인 가능성**:
1. Frontend 빌드가 제대로 안 됨
2. 캐시된 구버전 사용
3. Static 파일 복사 실패

**확인 방법**:
```bash
# Railway 로그에서 확인
grep -i "copyFrontend" deployment_logs.txt
grep -i "frontend/dist" deployment_logs.txt
```

**해결**:
- Railway Dashboard → **Deployments** → **Redeploy** 버튼 클릭

### 문제 2: 데이터가 안 보임 (Railway PostgreSQL 데이터 사용 안 함)

**원인 가능성**:
1. PostgreSQL 연결 실패
2. 환경변수 설정 안 됨
3. Profile 설정 오류 (railway profile 사용 안 함)

**확인 방법**:
```bash
# Railway 로그에서 확인
grep -i "postgresql" deployment_logs.txt
grep -i "datasource" deployment_logs.txt
grep -i "railway profile" deployment_logs.txt
```

**Railway 환경변수 확인**:
1. Railway Dashboard → 서비스 선택
2. **Variables** 탭
3. 다음 변수 확인:
   ```
   DATABASE_URL=postgresql://postgres:...@yamanote.proxy.rlwy.net:51273/railway
   PGHOST=yamanote.proxy.rlwy.net
   PGPORT=51273
   PGUSER=postgres
   PGPASSWORD=yOPQIglOJVBrJtUlCMVhVqLQLhEFLwXg
   PGDATABASE=railway
   ```

### 문제 3: 브라우저 캐시

**해결**:
1. 브라우저에서 **Ctrl+Shift+R** (하드 리프레시)
2. 또는 시크릿/프라이빗 모드로 접속

## 6. Railway 강제 재배포

### 방법 1: Railway Dashboard에서
1. **Deployments** 탭
2. 최신 배포 클릭
3. **⋮** 메뉴 → **Redeploy**

### 방법 2: 빈 commit으로 트리거
```bash
git commit --allow-empty -m "Force Railway redeploy"
git push
```

### 방법 3: 환경변수 추가/수정
1. **Variables** 탭
2. 새 변수 추가 (예: `FORCE_REBUILD=1`)
3. 자동으로 재배포 트리거

## 7. 로그 분석 명령어

### Railway CLI 설치 (선택)
```bash
npm i -g @railway/cli
railway login
railway logs
```

### 주요 로그 검색 키워드
```bash
# Frontend 빌드 확인
railway logs | grep -i "vite build"
railway logs | grep -i "dist"

# Backend 빌드 확인
railway logs | grep -i "gradle"
railway logs | grep -i "aiinsight-0.0.1"

# 실행 확인
railway logs | grep -i "started"
railway logs | grep -i "postgresql"
railway logs | grep -i "redis"

# 에러 확인
railway logs | grep -i "error"
railway logs | grep -i "failed"
railway logs | grep -i "exception"
```

## 8. 최종 확인 사항

### Backend 확인
- [ ] Health check 응답 (UP)
- [ ] PostgreSQL 연결 (yamanote.proxy.rlwy.net)
- [ ] Redis 연결 (interchange.proxy.rlwy.net)
- [ ] API 응답 (크롤링 타겟, 기사 목록)

### Frontend 확인
- [ ] Static 파일 서빙 (index.html)
- [ ] JavaScript 번들 로드
- [ ] API 호출 성공 (상대 경로 `/api`)
- [ ] 리포트 메뉴 표시
- [ ] 데이터 렌더링

### 데이터베이스 확인
- [ ] Railway PostgreSQL 사용 (H2 아님)
- [ ] 13개 크롤링 타겟 존재
- [ ] 기사 데이터 조회 가능

## 9. 긴급 디버깅

문제가 계속되면 다음 정보를 확인:

1. **Railway 배포 URL**: `_________________________`
2. **Health Check 응답**: `_________________________`
3. **API 응답 예시**: `_________________________`
4. **브라우저 Console 에러**: `_________________________`
5. **Railway 로그 에러**: `_________________________`

이 정보를 제공하면 정확한 문제 진단이 가능합니다.
