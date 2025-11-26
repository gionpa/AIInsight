package com.aiinsight.crawler;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "crawler")
@Getter
@Setter
public class CrawlerConfig {

    private String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";
    private int timeout = 10000;
    private int retryCount = 3;
    private int delayBetweenRequests = 1000; // 요청 간 딜레이 (ms)
}
