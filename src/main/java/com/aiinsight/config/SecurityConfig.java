package com.aiinsight.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
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
            .csrf().disable()
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()  // 모든 요청 허용 (임시)
            );

        return http.build();
    }
}

