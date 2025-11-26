package com.aiinsight.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@ConfigurationProperties(prefix = "ai")
@Getter
@Setter
public class AiConfig {

    private String provider = "claude-cli"; // claude-cli (headless), claude (API), 또는 openai

    private OpenAiConfig openai = new OpenAiConfig();
    private ClaudeConfig claude = new ClaudeConfig();
    private ClaudeCliConfig claudeCli = new ClaudeCliConfig();

    @Getter
    @Setter
    public static class OpenAiConfig {
        private String apiKey;
        private String model = "gpt-4o-mini";
        private String baseUrl = "https://api.openai.com/v1";
    }

    @Getter
    @Setter
    public static class ClaudeConfig {
        private String apiKey;
        private String model = "claude-3-haiku-20240307";
        private String baseUrl = "https://api.anthropic.com/v1";
    }

    @Getter
    @Setter
    public static class ClaudeCliConfig {
        private int timeout = 120; // 초 단위 타임아웃
        private String command = "claude"; // CLI 명령어 경로
    }

    @Bean
    public WebClient claudeWebClient() {
        return WebClient.builder()
                .baseUrl(claude.getBaseUrl())
                .defaultHeader("x-api-key", claude.getApiKey() != null ? claude.getApiKey() : "")
                .defaultHeader("anthropic-version", "2023-06-01")
                .defaultHeader("Content-Type", "application/json")
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }

    @Bean
    public WebClient openAiWebClient() {
        return WebClient.builder()
                .baseUrl(openai.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + (openai.getApiKey() != null ? openai.getApiKey() : ""))
                .defaultHeader("Content-Type", "application/json")
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }
}
