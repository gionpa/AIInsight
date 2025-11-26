# Railway 도메인 설정 가이드

## 🌐 Railway 자동 도메인

Railway는 배포 시 자동으로 무료 도메인을 제공합니다.

### 자동 생성 도메인
배포가 완료되면 Railway가 자동으로 다음 형식의 도메인을 할당합니다:
```
https://aiinsight-production.up.railway.app
```

### 도메인 확인 방법
1. **Railway Dashboard** 접속
2. 배포된 서비스 선택
3. **Settings** 탭 클릭
4. **Networking** 섹션에서 도메인 확인

배포된 도메인에서:
- 프론트엔드: `https://your-domain.up.railway.app/`
- API: `https://your-domain.up.railway.app/api`
- Health Check: `https://your-domain.up.railway.app/actuator/health`

---

## 🎯 커스텀 도메인 설정

자신의 도메인을 연결하려면 다음 단계를 따르세요.

### 1단계: Railway에서 커스텀 도메인 추가

1. **Railway Dashboard** → 서비스 선택
2. **Settings** → **Networking** 섹션
3. **Custom Domain** 버튼 클릭
4. 원하는 도메인 입력 (예: `aiinsight.yourdomain.com`)

### 2단계: DNS 설정

Railway가 제공하는 CNAME 레코드를 DNS에 추가해야 합니다.

#### 예시: Cloudflare DNS 설정
```
Type: CNAME
Name: aiinsight (또는 @, www 등)
Target: your-service.up.railway.app
Proxy: OFF (처음에는 OFF, 설정 완료 후 ON 가능)
TTL: Auto
```

#### 예시: Route53 (AWS) DNS 설정
```
Record type: CNAME
Record name: aiinsight
Value: your-service.up.railway.app
TTL: 300
```

#### 예시: Google Domains
```
Host name: aiinsight
Type: CNAME
TTL: 3600
Data: your-service.up.railway.app
```

### 3단계: SSL 인증서 자동 발급

DNS 설정이 완료되면:
- Railway가 자동으로 Let's Encrypt SSL 인증서 발급
- HTTPS 자동 적용 (5-10분 소요)
- 인증서 자동 갱신

### 4단계: 확인

```bash
# DNS 전파 확인
nslookup aiinsight.yourdomain.com

# HTTPS 확인
curl -I https://aiinsight.yourdomain.com/actuator/health
```

---

## 📝 도메인 종류별 설정

### 루트 도메인 (yourdomain.com)

**주의**: CNAME은 루트 도메인에 직접 사용할 수 없습니다.

**해결 방법**:
1. **CNAME Flattening 지원 DNS 사용** (Cloudflare 권장)
   ```
   Type: CNAME
   Name: @
   Target: your-service.up.railway.app
   Proxy: OFF → 설정 완료 후 ON
   ```

2. **A 레코드 사용** (Railway IP 주소 확인 필요)
   - Railway Dashboard에서 static IP 확인
   - A 레코드로 IP 주소 직접 연결

### 서브도메인 (aiinsight.yourdomain.com)

가장 간단하고 권장되는 방법:
```
Type: CNAME
Name: aiinsight
Target: your-service.up.railway.app
```

### www 도메인

```
Type: CNAME
Name: www
Target: your-service.up.railway.app
```

---

## 🔧 트러블슈팅

### 문제 1: DNS가 전파되지 않음

**증상**: 도메인 접속 시 "DNS_PROBE_FINISHED_NXDOMAIN" 오류

**해결**:
1. DNS 전파 대기 (최대 24-48시간)
2. DNS 캐시 클리어:
   ```bash
   # macOS
   sudo dscacheutil -flushcache; sudo killall -HUP mDNSResponder

   # Windows
   ipconfig /flushdns

   # Linux
   sudo systemd-resolve --flush-caches
   ```

3. DNS 전파 확인:
   - https://www.whatsmydns.net/

### 문제 2: SSL 인증서 발급 실패

**증상**: "Your connection is not private" 경고

**해결**:
1. DNS 설정 확인 (CNAME이 정확히 설정되었는지)
2. Cloudflare Proxy OFF로 변경
3. 5-10분 대기 후 다시 확인
4. Railway Dashboard에서 "Retry SSL" 클릭

### 문제 3: 502 Bad Gateway

**증상**: 도메인은 연결되지만 502 오류

**해결**:
1. Railway 서비스가 정상 실행 중인지 확인
2. Health check 엔드포인트 확인:
   ```bash
   curl https://your-domain.up.railway.app/actuator/health
   ```
3. 애플리케이션 로그 확인 (Railway Dashboard → Deployments → Logs)

### 문제 4: 프론트엔드는 되지만 API 호출 실패

**증상**: 페이지는 로드되지만 데이터가 없음

**해결**:
1. 브라우저 개발자 도구 → Network 탭 확인
2. CORS 설정 확인 (CorsConfig.java)
3. API URL이 올바른지 확인 (상대 경로 `/api` 사용)

---

## 🚀 배포 완료 체크리스트

### Railway 자동 도메인 사용 시
- [ ] Railway 배포 완료
- [ ] 자동 생성된 도메인 확인
- [ ] HTTPS 자동 적용 확인
- [ ] 프론트엔드 접속 테스트 (/)
- [ ] API 테스트 (/api)
- [ ] Health check 확인 (/actuator/health)

### 커스텀 도메인 사용 시
- [ ] Railway에서 커스텀 도메인 추가
- [ ] DNS에 CNAME 레코드 추가
- [ ] DNS 전파 확인 (24-48시간)
- [ ] SSL 인증서 자동 발급 확인
- [ ] HTTPS로 접속 가능한지 확인
- [ ] 프론트엔드 접속 테스트
- [ ] API 테스트
- [ ] Health check 확인

---

## 🔒 보안 권장사항

### 1. Cloudflare 사용 (권장)

Cloudflare를 DNS로 사용하면:
- **무료 CDN**: 전 세계 배포
- **DDoS 방어**: 자동 공격 차단
- **캐싱**: 정적 파일 캐싱
- **SSL/TLS**: 추가 보안 레이어

**설정 방법**:
1. Cloudflare에 도메인 등록
2. Nameserver를 Cloudflare로 변경
3. CNAME 레코드 추가
4. Proxy: ON (주황색 구름)

### 2. HTTP → HTTPS 리다이렉트

Spring Boot에서 자동 처리되지만, 확인:
```yaml
server:
  forward-headers-strategy: native
```

### 3. CORS 설정 확인

프로덕션 도메인을 CORS에 추가:
```java
@Configuration
public class CorsConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                    .allowedOrigins(
                        "https://your-domain.up.railway.app",
                        "https://aiinsight.yourdomain.com"
                    );
            }
        };
    }
}
```

---

## 📊 도메인 비용

### Railway 자동 도메인
- **비용**: 무료
- **SSL**: 무료 (Let's Encrypt)
- **제한**: Railway 제공 도메인만 사용 가능

### 커스텀 도메인
- **Railway 연결**: 무료
- **도메인 구입**: 연간 $10-$20 (등록 대행사에 따라 다름)
  - Namecheap: ~$10/년
  - Google Domains: ~$12/년
  - GoDaddy: ~$15/년
- **SSL**: 무료 (Railway가 Let's Encrypt 자동 발급)

### Cloudflare (선택사항)
- **무료 플랜**: DNS, CDN, DDoS 방어, SSL
- **Pro 플랜** ($20/월): 추가 보안 및 성능 기능

---

## 🎯 권장 설정

### 프로덕션 환경
```
도메인: aiinsight.yourdomain.com (커스텀 도메인)
DNS: Cloudflare (무료)
SSL: Let's Encrypt (Railway 자동)
CDN: Cloudflare Proxy ON
```

### 개발/테스트 환경
```
도메인: aiinsight-staging.up.railway.app (Railway 자동)
별도 Railway 프로젝트 사용
```

---

## 📞 추가 지원

- **Railway 문서**: https://docs.railway.app/deploy/exposing-your-app
- **Cloudflare 가이드**: https://developers.cloudflare.com/dns/
- **Let's Encrypt**: https://letsencrypt.org/

---

## ✅ 배포 성공 예시

**Railway 자동 도메인**:
```
✅ https://aiinsight-production.up.railway.app/
✅ https://aiinsight-production.up.railway.app/api
✅ https://aiinsight-production.up.railway.app/actuator/health
```

**커스텀 도메인 + Cloudflare**:
```
✅ https://aiinsight.yourdomain.com/
✅ https://aiinsight.yourdomain.com/api
✅ https://aiinsight.yourdomain.com/actuator/health
✅ SSL A+ Rating
✅ CDN 캐싱 활성화
✅ DDoS 방어 활성화
```
