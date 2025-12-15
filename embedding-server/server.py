"""
BGE 임베딩 서버 (Python FastAPI)
Railway CPU 환경을 위한 경량 임베딩 서버
"""

import os
import logging
from typing import List, Union
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import uvicorn
from sentence_transformers import SentenceTransformer

# 로깅 설정
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# 환경 변수
MODEL_NAME = os.getenv("MODEL_NAME", "BAAI/bge-small-en-v1.5")
PORT = int(os.getenv("PORT", 8081))
MAX_LENGTH = int(os.getenv("MAX_LENGTH", 512))

# FastAPI 앱 초기화
app = FastAPI(
    title="BGE Embedding Server",
    description="BAAI/bge-small-en-v1.5 embedding server for AIInsight",
    version="1.0.0"
)

# 전역 모델 변수
model = None


@app.on_event("startup")
async def load_model():
    """서버 시작 시 모델 로드"""
    global model
    try:
        logger.info(f"Loading model: {MODEL_NAME}")
        model = SentenceTransformer(MODEL_NAME)
        logger.info(f"Model loaded successfully. Embedding dimension: {model.get_sentence_embedding_dimension()}")
    except Exception as e:
        logger.error(f"Failed to load model: {e}")
        raise


class EmbeddingRequest(BaseModel):
    """임베딩 요청 모델"""
    input: Union[str, List[str]]
    model: str = MODEL_NAME


class EmbeddingResponse(BaseModel):
    """임베딩 응답 모델"""
    object: str = "list"
    data: List[dict]
    model: str
    usage: dict


@app.get("/")
async def root():
    """Health check endpoint"""
    return {
        "status": "ok",
        "model": MODEL_NAME,
        "dimension": model.get_sentence_embedding_dimension() if model else None
    }


@app.get("/health")
async def health():
    """상세 Health check"""
    if model is None:
        raise HTTPException(status_code=503, detail="Model not loaded")

    return {
        "status": "healthy",
        "model": MODEL_NAME,
        "dimension": model.get_sentence_embedding_dimension(),
        "max_length": MAX_LENGTH
    }


@app.post("/embeddings")
async def create_embeddings(request: EmbeddingRequest) -> EmbeddingResponse:
    """임베딩 생성 API (OpenAI 호환 형식)"""
    if model is None:
        raise HTTPException(status_code=503, detail="Model not loaded")

    try:
        # 입력 텍스트 처리
        texts = [request.input] if isinstance(request.input, str) else request.input

        if not texts:
            raise HTTPException(status_code=400, detail="Input cannot be empty")

        # 임베딩 생성
        logger.info(f"Generating embeddings for {len(texts)} text(s)")
        embeddings = model.encode(
            texts,
            normalize_embeddings=True,  # 코사인 유사도를 위한 정규화
            show_progress_bar=False
        )

        # OpenAI API 호환 형식으로 응답 구성
        data = [
            {
                "object": "embedding",
                "embedding": embedding.tolist(),
                "index": idx
            }
            for idx, embedding in enumerate(embeddings)
        ]

        # 토큰 사용량 추정 (대략 4자당 1토큰)
        total_tokens = sum(len(text) // 4 for text in texts)

        return EmbeddingResponse(
            data=data,
            model=MODEL_NAME,
            usage={
                "prompt_tokens": total_tokens,
                "total_tokens": total_tokens
            }
        )

    except Exception as e:
        logger.error(f"Error generating embeddings: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/embed")
async def embed(request: dict):
    """간단한 임베딩 엔드포인트 (text-embeddings-inference 호환)"""
    if model is None:
        raise HTTPException(status_code=503, detail="Model not loaded")

    try:
        inputs = request.get("inputs")
        if not inputs:
            raise HTTPException(status_code=400, detail="Missing 'inputs' field")

        # 단일 텍스트 또는 배열 처리
        texts = [inputs] if isinstance(inputs, str) else inputs

        # 임베딩 생성
        embeddings = model.encode(
            texts,
            normalize_embeddings=True,
            show_progress_bar=False
        )

        # 단일 텍스트면 1D 배열, 여러 텍스트면 2D 배열 반환
        if isinstance(inputs, str):
            return embeddings[0].tolist()
        else:
            return [emb.tolist() for emb in embeddings]

    except Exception as e:
        logger.error(f"Error in /embed endpoint: {e}")
        raise HTTPException(status_code=500, detail=str(e))


if __name__ == "__main__":
    logger.info(f"Starting BGE Embedding Server on port {PORT}")
    uvicorn.run(
        app,
        host="0.0.0.0",
        port=PORT,
        log_level="info"
    )
