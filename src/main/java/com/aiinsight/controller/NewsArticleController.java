package com.aiinsight.controller;

import com.aiinsight.domain.article.NewsArticle;
import com.aiinsight.dto.NewsArticleDto;
import com.aiinsight.service.AiSummaryService;
import com.aiinsight.service.NewsArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
@Tag(name = "뉴스 기사 관리", description = "수집된 뉴스 기사 조회 및 관리 API")
public class NewsArticleController {

    private final NewsArticleService newsArticleService;
    private final AiSummaryService aiSummaryService;

    @GetMapping
    @Operation(summary = "기사 목록 조회", description = "수집된 기사 목록을 페이징하여 조회합니다")
    public ResponseEntity<Page<NewsArticleDto.Response>> findAll(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(newsArticleService.findAll(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "기사 상세 조회", description = "특정 기사의 상세 정보(본문 포함)를 조회합니다")
    public ResponseEntity<NewsArticleDto.DetailResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(newsArticleService.findById(id));
    }

    @GetMapping("/new")
    @Operation(summary = "신규 기사 조회", description = "아직 읽지 않은 신규 기사 목록을 조회합니다")
    public ResponseEntity<Page<NewsArticleDto.Response>> findNewArticles(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(newsArticleService.findNewArticles(pageable));
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "카테고리별 기사 조회", description = "특정 카테고리의 기사 목록을 조회합니다")
    public ResponseEntity<Page<NewsArticleDto.Response>> findByCategory(
            @PathVariable NewsArticle.ArticleCategory category,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(newsArticleService.findByCategory(category, pageable));
    }

    @GetMapping("/target/{targetId}")
    @Operation(summary = "타겟별 기사 조회", description = "특정 크롤링 타겟에서 수집된 기사 목록을 조회합니다")
    public ResponseEntity<Page<NewsArticleDto.Response>> findByTargetId(
            @PathVariable Long targetId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(newsArticleService.findByTargetId(targetId, pageable));
    }

    @GetMapping("/search")
    @Operation(summary = "기사 검색", description = "키워드로 기사를 검색합니다")
    public ResponseEntity<Page<NewsArticleDto.Response>> search(
            @RequestParam String keyword,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(newsArticleService.search(keyword, pageable));
    }

    @GetMapping("/relevant")
    @Operation(summary = "관련성 높은 기사 조회", description = "관련성 점수가 특정 값 이상인 기사를 조회합니다")
    public ResponseEntity<Page<NewsArticleDto.Response>> findRelevantArticles(
            @RequestParam(defaultValue = "0.7") Double minScore,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(newsArticleService.findRelevantArticles(minScore, pageable));
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "기사 읽음 처리", description = "특정 기사를 읽음 상태로 변경합니다")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        newsArticleService.markAsRead(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/read")
    @Operation(summary = "다중 기사 읽음 처리", description = "여러 기사를 한번에 읽음 상태로 변경합니다")
    public ResponseEntity<Void> markAsRead(@RequestBody List<Long> ids) {
        newsArticleService.markAsRead(ids);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/count/new")
    @Operation(summary = "신규 기사 수 조회", description = "읽지 않은 신규 기사 수를 조회합니다")
    public ResponseEntity<Long> countNewArticles() {
        return ResponseEntity.ok(newsArticleService.countNewArticles());
    }

    @PostMapping("/{id}/analyze")
    @Operation(summary = "단일 기사 AI 분석", description = "특정 기사에 대해 AI 요약 및 중요도 분석을 실행합니다. force=true로 이미 분석된 기사도 재분석 가능")
    public ResponseEntity<NewsArticleDto.DetailResponse> analyzeArticle(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean force) {
        NewsArticle article = newsArticleService.findEntityById(id);
        aiSummaryService.summarizeArticle(article, force);
        return ResponseEntity.ok(newsArticleService.findById(id));
    }

    @PostMapping("/analyze-batch")
    @Operation(summary = "배치 AI 분석", description = "미분석 기사들에 대해 AI 분석을 일괄 실행합니다")
    public ResponseEntity<Map<String, Object>> analyzeBatch(
            @RequestParam(defaultValue = "10") int limit) {
        List<NewsArticle> unsummarized = newsArticleService.findUnsummarizedArticles(limit);
        int analyzed = 0;

        for (NewsArticle article : unsummarized) {
            try {
                aiSummaryService.summarizeArticle(article);
                analyzed++;
                Thread.sleep(1000); // API rate limit 방지
            } catch (Exception e) {
                // 개별 실패는 로깅만 하고 계속 진행
            }
        }

        return ResponseEntity.ok(Map.of(
                "requested", unsummarized.size(),
                "analyzed", analyzed,
                "message", analyzed + "개 기사 분석 완료"
        ));
    }

    @GetMapping("/importance/{importance}")
    @Operation(summary = "중요도별 기사 조회", description = "특정 중요도의 기사 목록을 조회합니다")
    public ResponseEntity<Page<NewsArticleDto.Response>> findByImportance(
            @PathVariable NewsArticle.ArticleImportance importance,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(newsArticleService.findByImportance(importance, pageable));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "기사 삭제", description = "특정 기사를 삭제합니다")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        newsArticleService.delete(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/batch")
    @Operation(summary = "다중 기사 삭제", description = "여러 기사를 한번에 삭제합니다")
    public ResponseEntity<Map<String, Object>> deleteBatch(@RequestBody List<Long> ids) {
        int deleted = newsArticleService.deleteBatch(ids);
        return ResponseEntity.ok(Map.of(
                "requested", ids.size(),
                "deleted", deleted,
                "message", deleted + "개 기사 삭제 완료"
        ));
    }
}
