package com.aiinsight.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 설정
 *
 * TODO: 인증 기능 구현 예정
 * - User 엔티티 이미 생성됨
 * - 나중에 네이버 OAuth2 또는 간단한 이메일 인증 추가 가능
 *
 * 현재: 모든 요청 허용 (개발 단계)
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF 비활성화 (REST API)
            .csrf(AbstractHttpConfigurer::disable)
            // 세션 사용 안함 (Stateless)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            // 모든 요청 허용 (개발 단계)
            .authorizeHttpRequests(auth -> auth
                // Actuator 엔드포인트는 항상 허용 (Railway health check)
                .requestMatchers("/actuator/**").permitAll()
                // 정적 리소스 허용
                .requestMatchers("/", "/index.html", "/assets/**", "/*.js", "/*.css", "/*.ico", "/*.svg").permitAll()
                // API 엔드포인트 허용
                .requestMatchers("/api/**").permitAll()
                // Swagger UI 허용
                .requestMatchers("/swagger-ui/**", "/api-docs/**", "/v3/api-docs/**").permitAll()
                // 나머지 모든 요청 허용 (개발 단계)
                .anyRequest().permitAll()
            );

        return http.build();
    }
}

