package com.aiinsight.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Spring Boot + React SPA 설정
 * - CORS 설정 (개발 환경)
 * - React Router 클라이언트 사이드 라우팅 지원
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:3000", "http://localhost:5173")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // React SPA를 위한 static 리소스 핸들러
        // 모든 non-API 요청을 index.html로 포워딩하여 React Router가 처리하도록 함
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requestedResource = location.createRelative(resourcePath);

                        // 1. 실제 파일이 존재하면 반환 (JS, CSS, images, favicon 등)
                        if (requestedResource.exists() && requestedResource.isReadable()) {
                            return requestedResource;
                        }

                        // 2. API 요청과 actuator는 컨트롤러로 전달 (index.html 반환 안 함)
                        if (resourcePath.startsWith("api/") || resourcePath.startsWith("actuator/")) {
                            return null;
                        }

                        // 3. 그 외 모든 요청은 index.html로 포워딩
                        // React Router가 /articles, /reports, /targets 등의 클라이언트 라우팅 처리
                        Resource indexHtml = new ClassPathResource("static/index.html");
                        if (indexHtml.exists() && indexHtml.isReadable()) {
                            return indexHtml;
                        }

                        return null;
                    }
                });
    }
}
