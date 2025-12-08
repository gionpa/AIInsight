# Railway용 Dockerfile - Chrome/Selenium 포함
# Multi-stage build로 최적화

# Stage 1: Frontend Build
FROM node:20-alpine AS frontend-builder

WORKDIR /app/frontend

# Frontend 의존성 설치
COPY frontend/package*.json ./
RUN npm ci

# Frontend 소스 복사 및 빌드
COPY frontend/ ./
RUN npm run build

# Stage 2: Backend Build
FROM gradle:8.5-jdk17 AS builder

WORKDIR /app

# Gradle 캐시 최적화를 위해 의존성 파일 먼저 복사
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# 의존성 다운로드 (캐시 활용)
RUN gradle dependencies --no-daemon || true

# 소스 코드 복사
COPY src ./src

# Frontend 빌드 결과 복사
COPY --from=frontend-builder /app/frontend/dist ./frontend/dist

# 정적 파일을 resources/static으로 복사
RUN mkdir -p src/main/resources/static && cp -r frontend/dist/* src/main/resources/static/

# JAR 빌드
RUN gradle bootJar --no-daemon -x test

# Stage 3: Runtime with Chrome
FROM --platform=linux/amd64 eclipse-temurin:17-jre-jammy

# 필수 패키지 및 Chrome 설치
RUN apt-get update && apt-get install -y --no-install-recommends \
    wget \
    gnupg \
    ca-certificates \
    fonts-liberation \
    libasound2 \
    libatk-bridge2.0-0 \
    libatk1.0-0 \
    libatspi2.0-0 \
    libcups2 \
    libdbus-1-3 \
    libdrm2 \
    libgbm1 \
    libgtk-3-0 \
    libnspr4 \
    libnss3 \
    libxcomposite1 \
    libxdamage1 \
    libxfixes3 \
    libxkbcommon0 \
    libxrandr2 \
    xdg-utils \
    libu2f-udev \
    libvulkan1 \
    dbus \
    procps \
    && rm -rf /var/lib/apt/lists/*

# Chrome 설치 (안정 버전)
RUN wget -q -O - https://dl-ssl.google.com/linux/linux_signing_key.pub | gpg --dearmor -o /usr/share/keyrings/googlechrome-linux-keyring.gpg \
    && echo "deb [arch=amd64 signed-by=/usr/share/keyrings/googlechrome-linux-keyring.gpg] http://dl.google.com/linux/chrome/deb/ stable main" > /etc/apt/sources.list.d/google-chrome.list \
    && apt-get update \
    && apt-get install -y --no-install-recommends google-chrome-stable \
    && rm -rf /var/lib/apt/lists/* \
    && ln -sf /usr/bin/google-chrome-stable /usr/bin/google-chrome \
    && ln -sf /usr/bin/google-chrome-stable /usr/bin/chromium

# Chrome 버전 확인 및 경로 출력
RUN echo "Chrome installed at: $(which google-chrome)" && google-chrome --version

# /dev/shm 크기 문제 해결을 위한 설정
RUN mkdir -p /dev/shm && chmod 1777 /dev/shm

WORKDIR /app

# JAR 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 정적 파일 복사
COPY --from=builder /app/frontend/dist ./frontend/dist

# Chrome이 root로 실행되어야 하므로 appuser 제거하고 root로 실행
# Railway 환경에서는 보안보다 기능이 우선

# 환경변수 설정
ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENV SPRING_PROFILES_ACTIVE=railway
ENV CHROME_BIN=/usr/bin/google-chrome
ENV CHROME_PATH=/usr/bin/google-chrome
# Chrome이 sandbox 없이 실행되도록 환경변수 설정
ENV CHROME_OPTS="--no-sandbox --disable-dev-shm-usage --disable-gpu"

# 포트 노출
EXPOSE 8080

# 헬스체크
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# 실행
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
