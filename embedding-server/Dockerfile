# Railway용 BGE 임베딩 서버 (Python 경량 버전)
# 메모리 효율적이고 안정적인 CPU 전용 솔루션

FROM python:3.11-slim

# 작업 디렉토리 설정
WORKDIR /app

# 시스템 패키지 업데이트 및 필수 패키지 설치
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

# pip 업그레이드 및 빌드 도구 설치
RUN pip install --no-cache-dir --upgrade pip setuptools wheel

# requirements.txt 복사
COPY requirements.txt /app/

# 패키지 개별 설치 (메모리 효율적 - 한번에 설치 시 OOM 방지)
# 1. 기본 의존성 먼저 설치
RUN pip install --no-cache-dir numpy

# 2. PyTorch CPU 버전 설치
RUN pip install --no-cache-dir torch --index-url https://download.pytorch.org/whl/cpu

# 3. sentence-transformers 설치
RUN pip install --no-cache-dir sentence-transformers

# 4. FastAPI와 uvicorn 설치
RUN pip install --no-cache-dir fastapi "uvicorn[standard]"

# 애플리케이션 코드 복사
COPY server.py /app/

# 환경 변수 설정
ENV MODEL_NAME=BAAI/bge-small-en-v1.5
ENV PORT=8081
ENV MAX_LENGTH=512
ENV PYTHONUNBUFFERED=1

# 포트 노출
EXPOSE 8081

# 서버 시작
CMD ["python", "server.py"]
