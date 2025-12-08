package com.aiinsight;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class AiInsightApplication {

    public static void main(String[] args) {
        // JVM 타임존을 Asia/Seoul로 설정 (Spring 초기화 전에 실행)
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
        SpringApplication.run(AiInsightApplication.class, args);
    }
}
