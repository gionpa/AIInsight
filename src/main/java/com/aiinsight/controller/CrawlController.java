package com.aiinsight.controller;

import com.aiinsight.crawler.CrawlResult;
import com.aiinsight.service.CrawlExecutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/crawl")
@RequiredArgsConstructor
@Tag(name = "크롤링 실행", description = "크롤링 수동 실행 API")
public class CrawlController {

    private final CrawlExecutionService crawlExecutionService;

    @PostMapping("/execute/{targetId}")
    @Operation(summary = "단일 타겟 크롤링 실행", description = "특정 크롤링 타겟을 즉시 실행합니다")
    public ResponseEntity<CrawlResultResponse> executeCrawl(@PathVariable Long targetId) {
        CrawlResult result = crawlExecutionService.executeCrawl(targetId);
        return ResponseEntity.ok(new CrawlResultResponse(
                result.isSuccess(),
                result.getArticleCount(),
                result.getDurationMs(),
                result.getErrorMessage()
        ));
    }

    @PostMapping("/execute-all")
    @Operation(summary = "전체 크롤링 실행", description = "활성화된 모든 크롤링 타겟을 순차적으로 실행합니다")
    public ResponseEntity<Void> executeAllCrawls() {
        // 비동기로 실행하지 않고 바로 실행 (필요시 @Async 추가 가능)
        new Thread(() -> crawlExecutionService.executeAllEnabledCrawls()).start();
        return ResponseEntity.accepted().build();
    }

    public record CrawlResultResponse(
            boolean success,
            int articlesFound,
            long durationMs,
            String errorMessage
    ) {}
}
