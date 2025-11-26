#!/bin/bash

echo "=== AI Insight 전체 빌드 ==="

# 프론트엔드 빌드
echo ">>> 프론트엔드 빌드 중..."
cd frontend
npm install
npm run build

# 프론트엔드 빌드 결과를 백엔드 static 폴더로 복사
echo ">>> 정적 파일 복사 중..."
rm -rf ../src/main/resources/static
mkdir -p ../src/main/resources/static
cp -r dist/* ../src/main/resources/static/

# 백엔드 빌드
echo ">>> 백엔드 빌드 중..."
cd ..
./gradlew clean build -x test

echo "=== 빌드 완료 ==="
echo "실행: java -jar build/libs/aiinsight-0.0.1-SNAPSHOT.jar"
