package com.aiinsight.service;

import com.aiinsight.domain.article.NewsArticle;
import com.aiinsight.domain.article.NewsArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Executive Summary 일별 리포트 생성 서비스
 * - 최근 7일간의 HIGH 중요도 기사 분석
 * - 오늘 추가된 기사에 가중치 부여
 * - Claude CLI를 활용한 경영진 요약 리포트 생성
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExecutiveSummaryService {

    private final NewsArticleRepository newsArticleRepository;

    /**
     * 특정 날짜의 Executive Summary 생성
     * @param targetDate 대상 날짜
     * @return Executive Summary (Markdown 형식)
     */
    public String generateExecutiveSummary(LocalDate targetDate) {
        log.info("Executive Summary 생성 시작: {}", targetDate);

        // 1. 최근 7일간의 HIGH 중요도 기사 조회
        LocalDateTime endOfDay = targetDate.plusDays(1).atStartOfDay();
        LocalDateTime startOfWeek = targetDate.minusDays(6).atStartOfDay(); // 7일 전부터

        List<NewsArticle> weeklyArticles = newsArticleRepository.findByImportanceAndCrawledAtBetweenOrderByCrawledAtDesc(
                NewsArticle.ArticleImportance.HIGH,
                startOfWeek,
                endOfDay
        );

        log.info("최근 7일간 HIGH 중요도 기사 수: {}", weeklyArticles.size());

        if (weeklyArticles.isEmpty()) {
            return generateEmptySummary(targetDate);
        }

        // 2. 오늘 추가된 기사와 과거 기사 분리
        LocalDateTime todayStart = targetDate.atStartOfDay();
        List<NewsArticle> todayArticles = weeklyArticles.stream()
                .filter(article -> article.getCrawledAt().isAfter(todayStart))
                .collect(Collectors.toList());

        List<NewsArticle> pastArticles = weeklyArticles.stream()
                .filter(article -> !article.getCrawledAt().isAfter(todayStart))
                .collect(Collectors.toList());

        log.info("오늘 기사: {}건, 과거 기사: {}건", todayArticles.size(), pastArticles.size());

        // 3. Claude CLI로 Executive Summary 생성
        String summary = generateSummaryWithClaude(targetDate, todayArticles, pastArticles);

        return summary;
    }

    /**
     * Claude CLI를 사용하여 Executive Summary 생성
     */
    private String generateSummaryWithClaude(LocalDate targetDate, List<NewsArticle> todayArticles, List<NewsArticle> pastArticles) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("당신은 AI 업계 전문 경영진 어드바이저입니다. 최근 1주일간의 주요 AI 뉴스를 분석하여 Executive Summary를 작성해주세요.\n\n");
        prompt.append(String.format("**리포트 날짜**: %s\n", targetDate.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일"))));
        prompt.append(String.format("**분석 기간**: %s ~ %s (최근 7일)\n\n",
                targetDate.minusDays(6).format(DateTimeFormatter.ofPattern("MM/dd")),
                targetDate.format(DateTimeFormatter.ofPattern("MM/dd"))));

        prompt.append("## 📊 데이터 요약\n\n");
        prompt.append(String.format("- 전체 HIGH 중요도 기사: %d건\n", todayArticles.size() + pastArticles.size()));
        prompt.append(String.format("- 오늘 추가된 기사: %d건 ⭐\n", todayArticles.size()));
        prompt.append(String.format("- 지난 6일간 기사: %d건\n\n", pastArticles.size()));

        // 오늘 기사 (가중치 높음)
        if (!todayArticles.isEmpty()) {
            prompt.append("## 🔥 오늘의 주요 뉴스 (우선 분석 대상)\n\n");
            int count = 1;
            for (NewsArticle article : todayArticles) {
                prompt.append(formatArticleForPrompt(article, count++, true));
            }
            prompt.append("\n");
        }

        // 과거 기사 (컨텍스트 제공)
        if (!pastArticles.isEmpty()) {
            prompt.append("## 📰 지난 6일간의 주요 뉴스 (배경 맥락)\n\n");
            int count = 1;
            // 최신 15개만 선택 (토큰 절약)
            List<NewsArticle> recentPastArticles = pastArticles.stream()
                    .limit(15)
                    .collect(Collectors.toList());

            for (NewsArticle article : recentPastArticles) {
                prompt.append(formatArticleForPrompt(article, count++, false));
            }
            prompt.append("\n");
        }

        prompt.append("## 📝 작성 가이드라인\n\n");
        prompt.append("**우선순위**: 오늘 추가된 기사를 중심으로 작성하되, 과거 기사로 맥락을 보강하세요.\n\n");
        prompt.append("다음 형식으로 Executive Summary를 작성해주세요:\n\n");
        prompt.append("### 🎯 Executive Summary\n");
        prompt.append("이번 주 AI 업계의 가장 중요한 변화와 핵심 메시지를 3-4문장으로 요약\n\n");
        prompt.append("### 📌 오늘의 핵심 이슈\n");
        prompt.append("오늘 추가된 뉴스 중 가장 중요한 2-3가지 이슈를 bullet point로 요약\n");
        prompt.append("- **이슈명**: 핵심 내용과 시사점 (1-2문장)\n\n");
        prompt.append("### 🔍 주간 주요 동향\n");
        prompt.append("이번 주 전체적인 트렌드를 3-5가지로 요약\n");
        prompt.append("- **트렌드명**: 관련 기사들의 공통 주제와 의미\n\n");
        prompt.append("### 💡 경영 시사점\n");
        prompt.append("경영진이 주목해야 할 전략적 포인트 2-3가지\n\n");
        prompt.append("**중요**: 반드시 Markdown 형식으로 작성하고, 오늘 추가된 기사를 우선적으로 강조해주세요.\n");

        // Claude CLI 호출
        try {
            return callClaudeCLI(prompt.toString());
        } catch (Exception e) {
            log.error("Claude CLI 호출 실패", e);
            return generateFallbackSummary(targetDate, todayArticles, pastArticles);
        }
    }

    /**
     * 기사를 프롬프트 형식으로 포맷팅
     */
    private String formatArticleForPrompt(NewsArticle article, int index, boolean isToday) {
        StringBuilder sb = new StringBuilder();

        String emoji = isToday ? "⭐ " : "";
        sb.append(String.format("%s**[%d]** %s\n", emoji, index, article.getTitleKo() != null ? article.getTitleKo() : article.getTitle()));
        sb.append(String.format("- 카테고리: %s | 관련성: %.2f\n", article.getCategory(), article.getRelevanceScore()));

        if (article.getUrgencyLevel() != null) {
            sb.append(String.format("- 긴급도: %s", article.getUrgencyLevel()));
        }
        if (article.getBusinessImpact() != null) {
            sb.append(String.format(" | 비즈니스 영향: %.2f", article.getBusinessImpact()));
        }
        if (article.getMentionedCompanies() != null && !article.getMentionedCompanies().isEmpty()) {
            sb.append(String.format(" | 관련 기업: %s", article.getMentionedCompanies()));
        }
        sb.append("\n");

        if (article.getSummary() != null && !article.getSummary().isEmpty()) {
            sb.append(String.format("- 요약: %s\n", article.getSummary()));
        }
        sb.append("\n");

        return sb.toString();
    }

    /**
     * Claude CLI 호출
     */
    private String callClaudeCLI(String prompt) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(
                "claude", "--print", prompt
        );
        processBuilder.redirectErrorStream(false);

        Process process = processBuilder.start();

        if (!process.waitFor(60, TimeUnit.SECONDS)) {
            process.destroy();
            throw new RuntimeException("Claude CLI timeout (60s)");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            try (BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                String errorOutput = errorReader.lines().collect(Collectors.joining("\n"));
                log.error("Claude CLI 실행 실패 (exit code: {}): {}", exitCode, errorOutput);
            }
            throw new RuntimeException("Claude CLI failed with exit code: " + exitCode);
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String output = reader.lines().collect(Collectors.joining("\n"));

            if (output == null || output.trim().isEmpty()) {
                throw new RuntimeException("Claude CLI returned empty output");
            }

            return output.trim();
        }
    }

    /**
     * 빈 요약 생성 (기사가 없을 때)
     */
    private String generateEmptySummary(LocalDate targetDate) {
        return String.format("""
                # Executive Summary - %s

                ## 📊 데이터 요약

                최근 7일간 HIGH 중요도 기사가 없습니다.

                ## 🎯 Executive Summary

                이번 주에는 주요한 AI 업계 뉴스가 보고되지 않았습니다. 다음 주를 기대해주세요.
                """,
                targetDate.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")));
    }

    /**
     * Fallback 요약 생성 (Claude CLI 실패 시)
     */
    private String generateFallbackSummary(LocalDate targetDate, List<NewsArticle> todayArticles, List<NewsArticle> pastArticles) {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("# Executive Summary - %s\n\n", targetDate.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일"))));
        sb.append("## 📊 데이터 요약\n\n");
        sb.append(String.format("- 전체 HIGH 중요도 기사: %d건\n", todayArticles.size() + pastArticles.size()));
        sb.append(String.format("- 오늘 추가된 기사: %d건\n", todayArticles.size()));
        sb.append(String.format("- 지난 6일간 기사: %d건\n\n", pastArticles.size()));

        sb.append("## 🎯 오늘의 주요 뉴스\n\n");
        if (todayArticles.isEmpty()) {
            sb.append("오늘 추가된 HIGH 중요도 기사가 없습니다.\n\n");
        } else {
            for (NewsArticle article : todayArticles) {
                sb.append(String.format("- **%s** (%s)\n",
                        article.getTitleKo() != null ? article.getTitleKo() : article.getTitle(),
                        article.getCategory()));
            }
            sb.append("\n");
        }

        sb.append("## 📌 주간 카테고리 분포\n\n");
        Map<NewsArticle.ArticleCategory, Long> categoryDistribution = pastArticles.stream()
                .collect(Collectors.groupingBy(NewsArticle::getCategory, Collectors.counting()));

        categoryDistribution.entrySet().stream()
                .sorted(Map.Entry.<NewsArticle.ArticleCategory, Long>comparingByValue().reversed())
                .forEach(entry -> sb.append(String.format("- %s: %d건\n", entry.getKey(), entry.getValue())));

        sb.append("\n*Note: Claude CLI를 통한 심층 분석을 이용할 수 없어 간소화된 요약을 제공합니다.*\n");

        return sb.toString();
    }
}
