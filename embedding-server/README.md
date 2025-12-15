# BGE 임베딩 서버 - Railway 배포 가이드

Railway에 BAAI/bge-small-en-v1.5 임베딩 서버를 배포하는 방법입니다.

## 1. Railway 프로젝트에 새 서비스 추가

### Railway Dashboard에서:
1. 프로젝트 선택 (aiinsight-production)
2. "New Service" 버튼 클릭
3. "Empty Service" 선택
4. 서비스 이름: `embedding-server`

## 2. GitHub 연동 배포

### 방법 A: 별도 저장소 생성 (권장)

1. 새 GitHub 저장소 생성: `aiinsight-embedding-server`
2. 이 디렉토리의 파일들을 저장소에 push:
```bash
cd embedding-server
git init
git add .
git commit -m "Initial commit: BGE embedding server"
git remote add origin https://github.com/YOUR_USERNAME/aiinsight-embedding-server.git
git push -u origin main
```

3. Railway에서 GitHub 연동:
   - embedding-server 서비스 선택
   - "Settings" → "Connect Repo"
   - 생성한 저장소 선택
   - Root Directory: `/` (루트)
   - Build Command: (비워둠 - Dockerfile 사용)

### 방법 B: 모노레포 방식

1. 현재 AIInsight 저장소 사용
2. Railway 설정:
   - Root Directory: `/embedding-server`
   - Build Command: (비워둠 - Dockerfile 사용)

## 3. 환경 변수 설정

Railway embedding-server 서비스에서:

```bash
MODEL_ID=BAAI/bge-small-en-v1.5
PORT=8081
MAX_CONCURRENT_REQUESTS=128
MAX_BATCH_TOKENS=16384
```

## 4. 메인 애플리케이션 환경 변수 설정

Railway aiinsight-production 서비스에서:

```bash
# 임베딩 서버 URL (같은 프로젝트 내부 URL 사용)
EMBEDDING_ENDPOINT=http://embedding-server.railway.internal:8081/embeddings
EMBEDDING_PROVIDER=local-bge
EMBEDDING_MODEL=BAAI/bge-small-en-v1.5
EMBEDDING_DIMENSION=384
```

**중요**: Railway 내부 네트워크를 사용하면 외부 인터넷을 거치지 않아 빠르고 안전합니다.

## 5. 배포 확인

### 임베딩 서버 상태 확인:
```bash
# Railway 외부 URL로 테스트
curl -X POST "https://embedding-server-production.up.railway.app/embeddings" \
  -H "Content-Type: application/json" \
  -d '{"input": "test", "model": "BAAI/bge-small-en-v1.5"}'
```

### 메인 애플리케이션 로그 확인:
```bash
railway logs --service aiinsight-production | grep -E "임베딩|embedding"
```

## 6. 테스트

크롤링 실행하여 임베딩 생성 확인:
```bash
curl -X POST "https://aiinsight-production.up.railway.app/api/crawl/execute/14"

# 30초 후 임베딩 확인
curl "https://aiinsight-production.up.railway.app/api/dashboard/stats"
```

## 리소스 요구사항

- **CPU**: 최소 0.5 vCPU (권장 1 vCPU)
- **RAM**: 최소 1GB (권장 2GB)
- **디스크**: 약 500MB (모델 다운로드)

## 비용

- Railway Hobby Plan: $5/월 (500시간 실행시간 + $5 리소스 크레딧)
- 임베딩 서버 예상 비용: ~$3-5/월 (상시 실행)

## 트러블슈팅

### 1. 모델 다운로드 실패
- Railway 로그 확인: `railway logs --service embedding-server`
- Hugging Face 접속 가능 여부 확인

### 2. 메모리 부족
- Railway 서비스 설정에서 메모리 증가 (Settings → Resources)

### 3. 연결 실패
- 내부 URL 확인: `http://embedding-server.railway.internal:8081`
- 방화벽 설정 확인

## 대안: Hugging Face Inference API

무료로 테스트하려면 Hugging Face Inference API 사용:

```bash
# Railway 환경변수
EMBEDDING_PROVIDER=openai  # OpenAI 호환 형식
EMBEDDING_ENDPOINT=https://api-inference.huggingface.co/models/BAAI/bge-small-en-v1.5
HUGGINGFACE_API_KEY=hf_xxxxxxxxxxxxx
```

단, 속도 제한(rate limit)이 있으므로 프로덕션에는 자체 서버 권장합니다.
