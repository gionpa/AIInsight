#!/bin/bash

echo "=== AI Insight DB 시작 (Podman) ==="

# 네트워크 생성 (없으면 생성)
podman network exists aiinsight-net 2>/dev/null || podman network create aiinsight-net

# 기존 컨테이너 정리
podman rm -f aiinsight-postgres aiinsight-redis 2>/dev/null

# PostgreSQL + pgvector 컨테이너 시작
echo ">>> PostgreSQL + pgvector 시작 중..."
podman run -d \
  --name aiinsight-postgres \
  --network aiinsight-net \
  -e POSTGRES_DB=aiinsight \
  -e POSTGRES_USER=aiinsight \
  -e POSTGRES_PASSWORD=aiinsight123 \
  -p 5432:5432 \
  -v aiinsight_postgres_data:/var/lib/postgresql/data \
  --restart unless-stopped \
  pgvector/pgvector:pg16

# Redis 컨테이너 시작
echo ">>> Redis 시작 중..."
podman run -d \
  --name aiinsight-redis \
  --network aiinsight-net \
  -p 6379:6379 \
  -v aiinsight_redis_data:/data \
  --restart unless-stopped \
  redis:7-alpine

# 잠시 대기 후 상태 확인
sleep 3

echo ""
echo "=== 컨테이너 상태 확인 ==="
podman ps --filter "name=aiinsight"

echo ""
echo "PostgreSQL: localhost:5432"
echo "  - Database: aiinsight"
echo "  - Username: aiinsight"
echo "  - Password: aiinsight123"
echo ""
echo "Redis: localhost:6379"
echo ""
echo "DB 접속: podman exec -it aiinsight-postgres psql -U aiinsight -d aiinsight"
