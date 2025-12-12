#!/usr/bin/env sh
set -euo pipefail

EMBED_MODEL="${EMBEDDING_MODEL:-BAAI/bge-small-en-v1.5}"
EMBED_ENDPOINT_PORT="${EMBEDDING_PORT:-8081}"

echo "Starting local FastAPI embedding server on port ${EMBED_ENDPOINT_PORT} with model ${EMBED_MODEL}..."
python3 -m uvicorn embedding_server:app \
  --host 0.0.0.0 \
  --port "${EMBED_ENDPOINT_PORT}" \
  --log-level warning &

EMBED_PID=$!
echo "Embedding server PID: ${EMBED_PID}"

echo "Starting Spring Boot app on port ${PORT:-8080}..."
exec java ${JAVA_OPTS:-"-Xmx512m -Xms256m -Duser.timezone=Asia/Seoul"} -jar /app/app.jar
