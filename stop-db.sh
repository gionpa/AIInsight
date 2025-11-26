#!/bin/bash

echo "=== AI Insight DB 중지 (Podman) ==="

podman stop aiinsight-postgres aiinsight-redis 2>/dev/null
podman rm aiinsight-postgres aiinsight-redis 2>/dev/null

echo "DB 컨테이너가 중지되었습니다."
echo ""
echo "데이터 볼륨 삭제 (초기화): podman volume rm aiinsight_postgres_data aiinsight_redis_data"
