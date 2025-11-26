package com.aiinsight.controller;

import com.aiinsight.crawler.CrawlResult;
import com.aiinsight.dto.CrawlTargetDto;
import com.aiinsight.service.CrawlExecutionService;
import com.aiinsight.service.CrawlTargetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/crawl-targets")
@RequiredArgsConstructor
@Tag(name = "크롤링 타겟 관리", description = "크롤링 대상 사이트 관리 API")
public class CrawlTargetController {

    private final CrawlTargetService crawlTargetService;
    private final CrawlExecutionService crawlExecutionService;

    @GetMapping
    @Operation(summary = "크롤링 타겟 목록 조회", description = "모든 크롤링 타겟 목록을 페이징하여 조회합니다")
    public ResponseEntity<Page<CrawlTargetDto.Response>> findAll(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(crawlTargetService.findAll(pageable));
    }

    @GetMapping("/all")
    @Operation(summary = "크롤링 타겟 전체 조회", description = "모든 크롤링 타겟을 조회합니다 (페이징 없음)")
    public ResponseEntity<List<CrawlTargetDto.Response>> findAllWithoutPaging() {
        return ResponseEntity.ok(crawlTargetService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "크롤링 타겟 상세 조회", description = "특정 크롤링 타겟의 상세 정보를 조회합니다")
    public ResponseEntity<CrawlTargetDto.Response> findById(@PathVariable Long id) {
        return ResponseEntity.ok(crawlTargetService.findById(id));
    }

    @PostMapping
    @Operation(summary = "크롤링 타겟 생성", description = "새로운 크롤링 타겟을 등록합니다")
    public ResponseEntity<CrawlTargetDto.Response> create(
            @Valid @RequestBody CrawlTargetDto.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(crawlTargetService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "크롤링 타겟 수정", description = "기존 크롤링 타겟 정보를 수정합니다")
    public ResponseEntity<CrawlTargetDto.Response> update(
            @PathVariable Long id,
            @Valid @RequestBody CrawlTargetDto.UpdateRequest request) {
        return ResponseEntity.ok(crawlTargetService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "크롤링 타겟 삭제", description = "크롤링 타겟을 삭제합니다")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        crawlTargetService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/toggle")
    @Operation(summary = "크롤링 타겟 활성화 토글", description = "크롤링 타겟의 활성화 상태를 전환합니다")
    public ResponseEntity<CrawlTargetDto.Response> toggleEnabled(@PathVariable Long id) {
        return ResponseEntity.ok(crawlTargetService.toggleEnabled(id));
    }

    @PostMapping("/{id}/crawl")
    @Operation(summary = "크롤링 실행", description = "특정 크롤링 타겟에 대해 즉시 크롤링을 실행합니다")
    public ResponseEntity<CrawlResult> executeCrawl(@PathVariable Long id) {
        CrawlResult result = crawlExecutionService.executeCrawl(id);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/crawl-all")
    @Operation(summary = "전체 크롤링 실행", description = "활성화된 모든 크롤링 타겟에 대해 크롤링을 실행합니다")
    public ResponseEntity<String> executeAllCrawls() {
        crawlExecutionService.executeAllEnabledCrawls();
        return ResponseEntity.ok("크롤링이 완료되었습니다");
    }
}
