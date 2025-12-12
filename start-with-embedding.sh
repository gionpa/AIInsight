#!/usr/bin/env sh
set -euo pipefail

EMBED_MODEL="${EMBEDDING_MODEL:-BAAI/bge-small-en-v1.5}"
EMBED_ENDPOINT_PORT="${EMBEDDING_PORT:-8081}"

echo "Starting text-embeddings-inference on port ${EMBED_ENDPOINT_PORT} with model ${EMBED_MODEL}..."
text-embeddings-inference \
  --model-id "${EMBED_MODEL}" \
  --port "${EMBED_ENDPOINT_PORT}" \
  --json-output \
  --max-client-batch-size 16 \
  --max-concurrent-requests 4 &

EMBED_PID=$!
echo "Embedding server PID: ${EMBED_PID}"

echo "Starting Spring Boot app on port ${PORT:-8080}..."
exec java ${JAVA_OPTS:-"-Xmx512m -Xms256m -Duser.timezone=Asia/Seoul"} -jar /app/app.jar
