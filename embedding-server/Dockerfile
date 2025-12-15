# Railway용 BGE 임베딩 서버 (Python 경량 버전)
# 메모리 효율적이고 안정적인 CPU 전용 솔루션

FROM python:3.11-slim

# 작업 디렉토리 설정
WORKDIR /app

# 시스템 패키지 업데이트 및 필수 패키지 설치
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

# Python 패키지 설치
RUN pip install --no-cache-dir \
    fastapi==0.104.1 \
    uvicorn[standard]==0.24.0 \
    sentence-transformers==2.2.2 \
    torch==2.1.0 --index-url https://download.pytorch.org/whl/cpu \
    numpy==1.24.3

# 애플리케이션 코드 복사
COPY server.py /app/

# 환경 변수 설정
ENV MODEL_NAME=BAAI/bge-small-en-v1.5
ENV PORT=8081
ENV MAX_LENGTH=512

# 포트 노출
EXPOSE 8081

# 서버 시작
CMD ["python", "server.py"]
