#!/usr/bin/env python3
import requests
import json

BASE_URL = "https://aiinsight-production.up.railway.app"

print("=" * 70)
print("Railway Frontend 진단 분석")
print("=" * 70)

# 1. 메인 HTML 확인
print("\n1️⃣  메인 HTML 파일 확인:")
try:
    response = requests.get(BASE_URL, timeout=10)
    print(f"   상태 코드: {response.status_code}")
    print(f"   Content-Type: {response.headers.get('Content-Type')}")
    
    html_content = response.text
    print(f"   HTML 길이: {len(html_content)} bytes")
    
    # index.html에 있어야 할 주요 요소 확인
    checks = {
        "Vite로 빌드됨": "type=\"module\"" in html_content,
        "React 루트": "id=\"root\"" in html_content or "id=\"app\"" in html_content,
        "JS 번들": ".js" in html_content and "src=" in html_content,
        "CSS 파일": ".css" in html_content and "href=" in html_content,
    }
    
    print("\n   HTML 내용 체크:")
    for check_name, result in checks.items():
        status = "✅" if result else "❌"
        print(f"   {status} {check_name}")
    
    # JS 파일 경로 추출
    import re
    js_files = re.findall(r'src="([^"]+\.js)"', html_content)
    css_files = re.findall(r'href="([^"]+\.css)"', html_content)
    
    if js_files:
        print(f"\n   발견된 JS 파일: {js_files}")
    if css_files:
        print(f"   발견된 CSS 파일: {css_files}")
        
except Exception as e:
    print(f"   ❌ 오류: {e}")

# 2. Static 파일 접근 테스트
print("\n2️⃣  Static 파일 접근 테스트:")
static_paths = [
    "/assets/index.js",
    "/assets/index.css",
    "/favicon.ico",
    "/vite.svg"
]

for path in static_paths:
    try:
        url = BASE_URL + path
        response = requests.get(url, timeout=5)
        size = len(response.content)
        status = "✅" if response.status_code == 200 else "❌"
        print(f"   {status} {path}: {response.status_code} ({size} bytes)")
    except Exception as e:
        print(f"   ❌ {path}: 오류 - {str(e)[:50]}")

# 3. API 엔드포인트 확인
print("\n3️⃣  API 엔드포인트 확인:")
api_tests = [
    "/actuator/health",
    "/api/articles?page=0&size=5",
    "/api/crawl-targets/all"
]

for path in api_tests:
    try:
        url = BASE_URL + path
        response = requests.get(url, timeout=10)
        print(f"   ✅ {path}: {response.status_code}")
        
        if path == "/api/articles?page=0&size=5":
            data = response.json()
            print(f"      → 총 기사: {data.get('totalElements')}개")
            
        elif path == "/api/crawl-targets/all":
            data = response.json()
            print(f"      → 총 타겟: {len(data)}개")
            
    except Exception as e:
        print(f"   ❌ {path}: {str(e)[:80]}")

# 4. CORS 및 헤더 확인
print("\n4️⃣  CORS 및 응답 헤더:")
try:
    response = requests.get(BASE_URL, timeout=10)
    headers_to_check = [
        "Access-Control-Allow-Origin",
        "Content-Security-Policy",
        "X-Frame-Options",
        "Cache-Control"
    ]
    
    for header in headers_to_check:
        value = response.headers.get(header, "없음")
        print(f"   {header}: {value}")
        
except Exception as e:
    print(f"   ❌ 오류: {e}")

# 5. 라우팅 테스트
print("\n5️⃣  React Router 라우팅 테스트:")
routes = ["/", "/articles", "/targets", "/reports"]
for route in routes:
    try:
        url = BASE_URL + route
        response = requests.get(url, timeout=5)
        status = "✅" if response.status_code == 200 else "❌"
        print(f"   {status} {route}: {response.status_code}")
    except Exception as e:
        print(f"   ❌ {route}: {str(e)[:50]}")

print("\n" + "=" * 70)
print("진단 완료")
print("=" * 70)
