package com.aiinsight.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/embeddings")
@RequiredArgsConstructor
@Slf4j
public class EmbeddingTestController {

    private final RestTemplate restTemplate;

    @Value("${ai.embedding.provider:local-bge}")
    private String embeddingProvider;

    @Value("${ai.embedding.model:BAAI/bge-m3}")
    private String embeddingModel;

    @Value("${ai.embedding.endpoint:http://localhost:8081/embeddings}")
    private String embeddingEndpoint;

    @Value("${ai.embedding.dimension:1024}")
    private int embeddingDimension;

    @GetMapping("/test-connection")
    public ResponseEntity<Map<String, Object>> testConnection() {
        Map<String, Object> response = new HashMap<>();

        try {
            log.info("임베딩 연결 테스트 시작");
            log.info("Provider: {}", embeddingProvider);
            log.info("Model: {}", embeddingModel);
            log.info("Endpoint: {}", embeddingEndpoint);
            log.info("Dimension: {}", embeddingDimension);

            response.put("provider", embeddingProvider);
            response.put("model", embeddingModel);
            response.put("endpoint", embeddingEndpoint);
            response.put("dimension", embeddingDimension);

            // 간단한 테스트 요청
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", embeddingModel);
            requestBody.put("input", "test");

            log.info("임베딩 서버로 테스트 요청 전송: {}", embeddingEndpoint);
            String embeddingResponse = restTemplate.postForObject(
                    embeddingEndpoint,
                    requestBody,
                    String.class
            );

            log.info("임베딩 서버 응답 성공");
            response.put("status", "success");
            response.put("serverResponse", embeddingResponse);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("임베딩 연결 테스트 실패", e);
            response.put("status", "error");
            response.put("error", e.getMessage());
            response.put("errorClass", e.getClass().getName());

            if (e.getCause() != null) {
                response.put("cause", e.getCause().getMessage());
            }

            return ResponseEntity.status(500).body(response);
        }
    }
}
