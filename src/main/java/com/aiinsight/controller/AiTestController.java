package com.aiinsight.controller;

import com.aiinsight.config.AiConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * AI 서비스 상태 테스트용 컨트롤러 (인증 불필요)
 */
@RestController
@RequestMapping("/api/ai-test")
@RequiredArgsConstructor
@Slf4j
public class AiTestController {

    private final AiConfig aiConfig;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("provider", aiConfig.getProvider());
        status.put("claudeCliCommand", aiConfig.getClaudeCli().getCommand());
        status.put("claudeCliTimeout", aiConfig.getClaudeCli().getTimeout());

        // 환경변수 확인
        String oauthToken = System.getenv("CLAUDE_CODE_OAUTH_TOKEN");
        status.put("oauthTokenSet", oauthToken != null && !oauthToken.isEmpty());
        status.put("oauthTokenLength", oauthToken != null ? oauthToken.length() : 0);

        return ResponseEntity.ok(status);
    }

    @GetMapping("/claude-test")
    public ResponseEntity<Map<String, Object>> testClaude() {
        Map<String, Object> result = new HashMap<>();
        result.put("provider", aiConfig.getProvider());

        try {
            String command = aiConfig.getClaudeCli().getCommand();
            int timeout = 30; // 테스트용 짧은 타임아웃

            log.info("Claude CLI 테스트 시작...");

            ProcessBuilder processBuilder = new ProcessBuilder(
                command,
                "--print",
                "--output-format", "text"
            );

            processBuilder.environment().put("LANG", "ko_KR.UTF-8");
            processBuilder.environment().put("LC_ALL", "ko_KR.UTF-8");
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            // 간단한 테스트 프롬프트
            String testPrompt = "Say 'Hello, Claude CLI is working!' in exactly those words.";
            try (OutputStreamWriter writer = new OutputStreamWriter(
                    process.getOutputStream(), StandardCharsets.UTF_8)) {
                writer.write(testPrompt);
                writer.flush();
            }

            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(timeout, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                result.put("success", false);
                result.put("error", "Timeout after " + timeout + " seconds");
                return ResponseEntity.ok(result);
            }

            int exitCode = process.exitValue();
            result.put("exitCode", exitCode);
            result.put("response", response.toString().trim());
            result.put("success", exitCode == 0);

            if (exitCode != 0) {
                result.put("error", "Exit code: " + exitCode);
            }

            log.info("Claude CLI 테스트 완료: exitCode={}, response={}", exitCode, response.toString().trim());

        } catch (Exception e) {
            log.error("Claude CLI 테스트 실패: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("errorType", e.getClass().getSimpleName());
        }

        return ResponseEntity.ok(result);
    }
}
