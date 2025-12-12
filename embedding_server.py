import os
from typing import List, Union

from fastapi import FastAPI
from pydantic import BaseModel
from fastembed import TextEmbedding

MODEL_ID = os.getenv("EMBEDDING_MODEL", "BAAI/bge-small-en-v1.5")

# FastEmbed는 lazy 로드/캐시를 사용하므로 최초 요청 시 다운로드가 발생할 수 있음
embed_model = TextEmbedding(model_name=MODEL_ID)

app = FastAPI(title="Local Embedding Server", version="1.0.0")


class EmbeddingRequest(BaseModel):
    model: str | None = None
    input: Union[str, List[str]]


@app.post("/embeddings")
async def create_embeddings(req: EmbeddingRequest):
    # 입력을 리스트 형태로 정규화
    inputs = req.input if isinstance(req.input, list) else [req.input]

    embeddings = list(embed_model.embed(inputs))
    data = []
    for idx, emb in enumerate(embeddings):
        data.append(
            {
                "object": "embedding",
                "embedding": emb,
                "index": idx,
            }
        )

    return {
        "object": "list",
        "data": data,
        "model": req.model or MODEL_ID,
    }
