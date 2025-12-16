package com.aiinsight.service;

import com.aiinsight.domain.article.NewsArticle;
import com.aiinsight.domain.article.NewsArticleRepository;
import com.aiinsight.dto.ReportDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReportService {

    private final NewsArticleRepository newsArticleRepository;

    private static final Map<String, String> CATEGORY_DISPLAY_NAMES = Map.ofEntries(
            Map.entry("LLM", "대규모 언어 모델"),
            Map.entry("COMPUTER_VISION", "컴퓨터 비전"),
            Map.entry("NLP", "자연어 처리"),
            Map.entry("ROBOTICS", "로보틱스"),
            Map.entry("ML_OPS", "ML Ops"),
            Map.entry("RESEARCH", "연구/논문"),
            Map.entry("INDUSTRY", "산업 동향"),
            Map.entry("STARTUP", "스타트업"),
            Map.entry("REGULATION", "규제/정책"),
            Map.entry("TUTORIAL", "튜토리얼"),
            Map.entry("PRODUCT", "제품/서비스"),
            Map.entry("OTHER", "기타")
    );

    /**
     * 오늘의 HIGH 중요도 기사 리포트 생성
     */
    public ReportDto.DailyReport generateDailyReport() {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();

        // HIGH 중요도 기사 조회
        Page<NewsArticle> highImportanceArticles = newsArticleRepository
                .findByImportanceOrderByCrawledAtDesc(
                        NewsArticle.ArticleImportance.HIGH,
                        PageRequest.of(0, 50)
                );

        List<NewsArticle> articles = highImportanceArticles.getContent();

        // 카테고리별 분포
        Map<String, Integer> categoryDistribution = articles.stream()
                .filter(a -> a.getCategory() != null)
                .collect(Collectors.groupingBy(
                        a -> a.getCategory().name(),
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));

        // 기사 요약 목록 생성
        List<ReportDto.ArticleSummary> articleSummaries = articles.stream()
                .map(ReportDto.ArticleSummary::from)
                .collect(Collectors.toList());

        // Executive Summary 생성
        String executiveSummary = generateExecutiveSummary(articles, categoryDistribution);

        return ReportDto.DailyReport.builder()
                .generatedAt(LocalDateTime.now())
                .period(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .totalHighImportanceArticles(articles.size())
                .categoryDistribution(categoryDistribution)
                .articles(articleSummaries)
                .executiveSummary(executiveSummary)
                .build();
    }

    /**
     * 카테고리별 리포트 생성
     */
    public List<ReportDto.CategoryReport> generateCategoryReport() {
        Page<NewsArticle> highImportanceArticles = newsArticleRepository
                .findByImportanceOrderByCrawledAtDesc(
                        NewsArticle.ArticleImportance.HIGH,
                        PageRequest.of(0, 100)
                );

        // 카테고리별로 그룹화
        Map<NewsArticle.ArticleCategory, List<NewsArticle>> byCategory = highImportanceArticles.getContent()
                .stream()
                .filter(a -> a.getCategory() != null)
                .collect(Collectors.groupingBy(NewsArticle::getCategory));

        List<ReportDto.CategoryReport> reports = new ArrayList<>();

        for (Map.Entry<NewsArticle.ArticleCategory, List<NewsArticle>> entry : byCategory.entrySet()) {
            String categoryName = entry.getKey().name();
            List<ReportDto.ArticleSummary> summaries = entry.getValue().stream()
                    .sorted((a, b) -> Double.compare(
                            b.getRelevanceScore() != null ? b.getRelevanceScore() : 0,
                            a.getRelevanceScore() != null ? a.getRelevanceScore() : 0
                    ))
                    .map(ReportDto.ArticleSummary::from)
                    .collect(Collectors.toList());

            reports.add(ReportDto.CategoryReport.builder()
                    .category(categoryName)
                    .categoryDisplayName(CATEGORY_DISPLAY_NAMES.getOrDefault(categoryName, categoryName))
                    .articleCount(summaries.size())
                    .articles(summaries)
                    .build());
        }

        // 기사 수가 많은 순으로 정렬
        reports.sort((a, b) -> Integer.compare(b.getArticleCount(), a.getArticleCount()));

        return reports;
    }

    /**
     * 특정 카테고리의 HIGH 중요도 기사 조회
     */
    public ReportDto.CategoryReport getReportByCategory(String category) {
        NewsArticle.ArticleCategory articleCategory;
        try {
            articleCategory = NewsArticle.ArticleCategory.valueOf(category.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 카테고리: " + category);
        }

        Page<NewsArticle> articles = newsArticleRepository
                .findByCategoryAndImportanceOrderByRelevanceScoreDesc(
                        articleCategory,
                        NewsArticle.ArticleImportance.HIGH,
                        PageRequest.of(0, 50)
                );

        List<ReportDto.ArticleSummary> summaries = articles.getContent().stream()
                .map(ReportDto.ArticleSummary::from)
                .collect(Collectors.toList());

        return ReportDto.CategoryReport.builder()
                .category(category.toUpperCase())
                .categoryDisplayName(CATEGORY_DISPLAY_NAMES.getOrDefault(category.toUpperCase(), category))
                .articleCount(summaries.size())
                .articles(summaries)
                .build();
    }

    /**
     * Executive Summary 자동 생성
     * - A4 절반 분량 (~1000자)의 구조화된 리포트
     * - 핵심 요약, 주요 동향, 트렌드 인사이트, 향후 전망
     */
    private String generateExecutiveSummary(List<NewsArticle> articles, Map<String, Integer> categoryDistribution) {
        if (articles.isEmpty()) {
            return "## Executive Summary\n\n오늘 수집된 중요 AI 뉴스가 없습니다.";
        }

        StringBuilder summary = new StringBuilder();

        // === 1. 핵심 요약 ===
        summary.append("## 핵심 요약\n\n");
        summary.append(generateCoreSummary(articles, categoryDistribution));
        summary.append("\n\n");

        // === 2. 주요 동향 (상위 3개 카테고리) ===
        summary.append("## 주요 동향\n\n");
        summary.append(generateTopCategoryInsights(articles, categoryDistribution));
        summary.append("\n\n");

        // === 3. 트렌드 인사이트 ===
        summary.append("## 트렌드 인사이트\n\n");
        summary.append(generateTrendInsights(articles, categoryDistribution));
        summary.append("\n\n");

        // === 4. 주목할 만한 기사 ===
        summary.append("## 주목할 만한 기사\n\n");
        summary.append(generateHighlightArticles(articles));
        summary.append("\n\n");

        // === 5. 향후 전망 ===
        summary.append("## 향후 전망\n\n");
        summary.append(generateOutlook(articles, categoryDistribution));

        return summary.toString();
    }

    /**
     * 핵심 요약 생성 (2-3문장)
     */
    private String generateCoreSummary(List<NewsArticle> articles, Map<String, Integer> categoryDistribution) {
        StringBuilder core = new StringBuilder();

        // 전체 통계
        core.append(String.format("오늘 총 **%d개**의 중요 AI 뉴스가 수집되었으며, ", articles.size()));

        // 상위 카테고리
        Optional<Map.Entry<String, Integer>> topCategory = categoryDistribution.entrySet().stream()
                .max(Map.Entry.comparingByValue());

        if (topCategory.isPresent()) {
            String catName = CATEGORY_DISPLAY_NAMES.getOrDefault(topCategory.get().getKey(), topCategory.get().getKey());
            core.append(String.format("**%s** 분야에서 가장 활발한 움직임이 포착되었습니다(%d건). ",
                    catName, topCategory.get().getValue()));
        }

        // 높은 관련성 기사 비율
        long highRelevanceCount = articles.stream()
                .filter(a -> a.getRelevanceScore() != null && a.getRelevanceScore() >= 0.8)
                .count();

        if (highRelevanceCount > 0) {
            double percentage = (highRelevanceCount * 100.0) / articles.size();
            core.append(String.format("전체 기사 중 %.0f%%가 높은 AI 관련성(0.8 이상)을 보였으며, " +
                    "AI 업계의 핵심 이슈들이 집중적으로 다뤄지고 있음을 시사합니다.", percentage));
        }

        return core.toString();
    }

    /**
     * 상위 3개 카테고리별 심층 분석
     */
    private String generateTopCategoryInsights(List<NewsArticle> articles, Map<String, Integer> categoryDistribution) {
        StringBuilder insights = new StringBuilder();

        // 상위 3개 카테고리 선정
        List<Map.Entry<String, Integer>> topCategories = categoryDistribution.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(3)
                .collect(Collectors.toList());

        int index = 1;
        for (Map.Entry<String, Integer> entry : topCategories) {
            String categoryKey = entry.getKey();
            String categoryName = CATEGORY_DISPLAY_NAMES.getOrDefault(categoryKey, categoryKey);
            int count = entry.getValue();

            insights.append(String.format("### %d. %s (%d건)\n\n", index++, categoryName, count));

            // 해당 카테고리의 기사 필터링
            List<NewsArticle> categoryArticles = articles.stream()
                    .filter(a -> a.getCategory() != null && a.getCategory().name().equals(categoryKey))
                    .sorted((a, b) -> Double.compare(
                            b.getRelevanceScore() != null ? b.getRelevanceScore() : 0,
                            a.getRelevanceScore() != null ? a.getRelevanceScore() : 0
                    ))
                    .limit(2)
                    .collect(Collectors.toList());

            // 카테고리 설명 생성
            insights.append(generateCategoryDescription(categoryKey, categoryArticles));
            insights.append("\n\n");

            // 대표 기사 언급
            if (!categoryArticles.isEmpty()) {
                NewsArticle topArticle = categoryArticles.get(0);
                insights.append(String.format("**주요 기사**: \"%s\"\n\n",
                        topArticle.getTitleKo() != null ? topArticle.getTitleKo() : topArticle.getTitle()));

                if (topArticle.getSummary() != null && !topArticle.getSummary().isEmpty()) {
                    String shortSummary = topArticle.getSummary().length() > 150
                            ? topArticle.getSummary().substring(0, 150) + "..."
                            : topArticle.getSummary();
                    insights.append(shortSummary).append("\n\n");
                }
            }
        }

        return insights.toString();
    }

    /**
     * 카테고리별 맥락 설명 생성
     */
    private String generateCategoryDescription(String categoryKey, List<NewsArticle> articles) {
        return switch (categoryKey) {
            case "LLM" -> String.format(
                    "대규모 언어 모델 분야에서 %d건의 주요 발표와 기술 혁신이 있었습니다. " +
                            "GPT, Claude, Gemini 등 주요 모델의 성능 향상과 새로운 활용 사례가 주목받고 있으며, " +
                            "특히 멀티모달 기능과 추론 능력 개선이 두드러집니다.",
                    articles.size()
            );
            case "COMPUTER_VISION" -> String.format(
                    "컴퓨터 비전 기술이 %d건의 뉴스로 활발히 발전하고 있습니다. " +
                            "실시간 객체 인식, 3D 재구성, 의료 영상 분석 등 실용적 응용이 확대되고 있으며, " +
                            "엣지 디바이스에서의 효율적 추론이 산업 현장 적용을 가속화하고 있습니다.",
                    articles.size()
            );
            case "NLP" -> String.format(
                    "자연어 처리 분야에서 %d건의 혁신적 연구 성과가 발표되었습니다. " +
                            "번역, 요약, 감정 분석 등 전통적 NLP 태스크의 성능이 지속적으로 향상되고 있으며, " +
                            "특히 저자원 언어와 도메인 특화 모델에 대한 관심이 증가하고 있습니다.",
                    articles.size()
            );
            case "ROBOTICS" -> String.format(
                    "로보틱스 분야가 %d건의 뉴스로 주목받고 있습니다. " +
                            "AI 기반 로봇 제어, 자율주행, 협업 로봇(Cobot) 기술이 발전하며, " +
                            "제조업과 물류 자동화에서 실질적인 성과를 내고 있습니다.",
                    articles.size()
            );
            case "ML_OPS" -> String.format(
                    "MLOps 분야에서 %d건의 도구와 방법론 발전이 보고되었습니다. " +
                            "모델 배포, 모니터링, 버전 관리의 자동화가 성숙해지며, " +
                            "엔터프라이즈 환경에서 AI 시스템의 안정적 운영이 가능해지고 있습니다.",
                    articles.size()
            );
            case "RESEARCH" -> String.format(
                    "AI 연구 분야에서 %d건의 논문과 이론적 발전이 발표되었습니다. " +
                            "새로운 아키텍처, 학습 알고리즘, 평가 방법론이 제안되며, " +
                            "학계와 산업계의 협력이 강화되고 있습니다.",
                    articles.size()
            );
            case "INDUSTRY" -> String.format(
                    "AI 산업 동향에서 %d건의 주요 비즈니스 소식이 전해졌습니다. " +
                            "투자, 인수합병, 파트너십 등 산업 재편이 활발하며, " +
                            "AI 기술의 상업화와 시장 확대가 가속화되고 있습니다.",
                    articles.size()
            );
            case "STARTUP" -> String.format(
                    "AI 스타트업 생태계에서 %d건의 혁신 소식이 발표되었습니다. " +
                            "신규 투자 유치, 제품 출시, 시장 진입 전략이 주목받으며, " +
                            "새로운 AI 응용 분야가 지속적으로 개척되고 있습니다.",
                    articles.size()
            );
            case "REGULATION" -> String.format(
                    "AI 규제 및 정책 분야에서 %d건의 중요 결정이 내려졌습니다. " +
                            "EU AI Act, 미국 행정명령 등 글로벌 규제 프레임워크가 구체화되며, " +
                            "윤리, 안전, 개인정보 보호가 핵심 이슈로 부상하고 있습니다.",
                    articles.size()
            );
            case "PRODUCT" -> String.format(
                    "AI 제품 및 서비스 분야에서 %d건의 신제품 출시와 업데이트가 있었습니다. " +
                            "사용자 경험 개선, 새로운 기능 추가, 가격 정책 변화 등이 주요 트렌드이며, " +
                            "AI 기술의 대중화가 빠르게 진행되고 있습니다.",
                    articles.size()
            );
            default -> String.format(
                    "해당 분야에서 %d건의 주요 뉴스가 보고되었습니다. " +
                            "AI 기술의 다양한 응용과 발전이 지속적으로 이루어지고 있습니다.",
                    articles.size()
            );
        };
    }

    /**
     * 트렌드 인사이트 생성
     */
    private String generateTrendInsights(List<NewsArticle> articles, Map<String, Integer> categoryDistribution) {
        StringBuilder trends = new StringBuilder();

        // 카테고리 다양성 분석
        int uniqueCategories = categoryDistribution.size();
        trends.append(String.format("- **카테고리 다양성**: 총 %d개의 서로 다른 AI 분야에서 뉴스가 발생하여, " +
                "AI 생태계의 다각적 성장을 보여줍니다.\n\n", uniqueCategories));

        // 평균 관련성 점수
        double avgRelevance = articles.stream()
                .filter(a -> a.getRelevanceScore() != null)
                .mapToDouble(NewsArticle::getRelevanceScore)
                .average()
                .orElse(0.0);

        trends.append(String.format("- **AI 관련성**: 평균 관련성 점수 %.2f로, ", avgRelevance));
        if (avgRelevance >= 0.85) {
            trends.append("오늘의 뉴스들이 AI 핵심 기술과 직접적으로 연관되어 있어 업계 종사자들에게 높은 가치를 제공합니다.\n\n");
        } else if (avgRelevance >= 0.7) {
            trends.append("대부분의 뉴스가 AI와 밀접한 관련이 있어 유의미한 인사이트를 제공합니다.\n\n");
        } else {
            trends.append("다양한 관점에서 AI 관련 이슈가 다뤄지고 있습니다.\n\n");
        }

        // 핵심 키워드 트렌드 분석 (새로운 섹션)
        trends.append("- **핵심 키워드 트렌드**: ");
        List<String> trendKeywords = extractTrendKeywords(articles);
        if (!trendKeywords.isEmpty()) {
            trends.append(String.join(", ", trendKeywords))
                    .append(" 등이 주요 화두로 떠오르고 있습니다.\n\n");
        } else {
            trends.append("다양한 기술 키워드가 고르게 분포되어 있습니다.\n\n");
        }

        // 최신성 분석
        long todayArticles = articles.stream()
                .filter(a -> a.getCrawledAt().toLocalDate().equals(LocalDateTime.now().toLocalDate()))
                .count();

        trends.append(String.format("- **최신성**: 오늘 수집된 기사가 %d건(%.0f%%)으로, ",
                todayArticles, (todayArticles * 100.0) / articles.size()));

        if (todayArticles >= articles.size() * 0.7) {
            trends.append("실시간 AI 동향을 신속하게 파악할 수 있습니다.\n\n");
        } else {
            trends.append("최근 며칠간의 중요 이슈를 종합적으로 조망합니다.\n\n");
        }

        return trends.toString();
    }

    /**
     * 의미 있는 트렌드 키워드 추출
     * - "새로운", "AI", "개발" 등 뻔한 키워드 제외
     * - 기술 용어, 제품명, 기업명 등 구체적 키워드 추출
     */
    private List<String> extractTrendKeywords(List<NewsArticle> articles) {
        // 제외할 일반적인 키워드 (불용어)
        Set<String> stopWords = Set.of(
                "ai", "인공지능", "개발", "발표", "출시", "공개", "새로운", "최신", "기술", "시스템",
                "서비스", "플랫폼", "솔루션", "기업", "회사", "국내", "글로벌", "연구", "분석"
        );

        // 키워드 추출을 위해 제목과 요약에서 명사구 추출
        Map<String, Integer> keywordFrequency = new HashMap<>();

        for (NewsArticle article : articles) {
            String text = (article.getTitleKo() != null ? article.getTitleKo() : article.getTitle()) + " " +
                    (article.getSummary() != null ? article.getSummary() : "");

            // 간단한 키워드 추출 (2-4글자 단어)
            String[] words = text.split("[\\s,\\.\\-\\(\\)\\[\\]\"']+");
            for (String word : words) {
                String cleanWord = word.trim().toLowerCase();
                // 길이 체크 및 불용어 제외
                if (cleanWord.length() >= 2 && cleanWord.length() <= 15 &&
                        !stopWords.contains(cleanWord) &&
                        !cleanWord.matches(".*[0-9]+.*")) { // 숫자 포함 단어 제외

                    keywordFrequency.put(cleanWord, keywordFrequency.getOrDefault(cleanWord, 0) + 1);
                }
            }
        }

        // 빈도수 기준 상위 5개 키워드 선정 (최소 2회 이상 등장)
        return keywordFrequency.entrySet().stream()
                .filter(entry -> entry.getValue() >= 2)
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * 주목할 만한 기사 선정
     */
    private String generateHighlightArticles(List<NewsArticle> articles) {
        StringBuilder highlights = new StringBuilder();

        // 관련성 점수가 높은 상위 3개 기사
        List<NewsArticle> topArticles = articles.stream()
                .filter(a -> a.getRelevanceScore() != null)
                .sorted((a, b) -> Double.compare(
                        b.getRelevanceScore() != null ? b.getRelevanceScore() : 0,
                        a.getRelevanceScore() != null ? a.getRelevanceScore() : 0
                ))
                .limit(3)
                .collect(Collectors.toList());

        if (topArticles.isEmpty()) {
            highlights.append("오늘은 특별히 주목할 만한 기사가 선정되지 않았습니다.\n");
            return highlights.toString();
        }

        for (int i = 0; i < topArticles.size(); i++) {
            NewsArticle article = topArticles.get(i);
            highlights.append(String.format("%d. **%s** (관련성: %.2f)\n",
                    i + 1,
                    article.getTitleKo() != null ? article.getTitleKo() : article.getTitle(),
                    article.getRelevanceScore()
            ));

            if (article.getSummary() != null && !article.getSummary().isEmpty()) {
                String shortSummary = article.getSummary().length() > 120
                        ? article.getSummary().substring(0, 120) + "..."
                        : article.getSummary();
                highlights.append("   - ").append(shortSummary).append("\n");
            }
            highlights.append("\n");
        }

        return highlights.toString();
    }

    /**
     * 향후 전망 생성
     */
    private String generateOutlook(List<NewsArticle> articles, Map<String, Integer> categoryDistribution) {
        StringBuilder outlook = new StringBuilder();

        // 가장 활발한 분야 기반 전망
        Optional<Map.Entry<String, Integer>> topCategory = categoryDistribution.entrySet().stream()
                .max(Map.Entry.comparingByValue());

        if (topCategory.isPresent()) {
            String categoryKey = topCategory.get().getKey();
            String categoryName = CATEGORY_DISPLAY_NAMES.getOrDefault(categoryKey, categoryKey);

            outlook.append(String.format("**%s** 분야의 활발한 움직임은 ", categoryName));

            switch (categoryKey) {
                case "LLM" -> outlook.append("향후 몇 주간 GPT-5, Claude 4 등 차세대 모델 출시와 관련된 " +
                        "추가 발표가 예상되며, 멀티모달 AI의 상용화가 본격화될 것으로 전망됩니다.");
                case "COMPUTER_VISION" -> outlook.append("자율주행과 로보틱스 분야의 실용화가 가속화되고, " +
                        "산업 현장에서의 AI 비전 시스템 도입이 확대될 것으로 보입니다.");
                case "REGULATION" -> outlook.append("글로벌 AI 규제가 구체화되면서 기업들의 컴플라이언스 대응이 " +
                        "핵심 이슈로 부상할 것이며, 규제 준수 솔루션 시장이 성장할 것으로 예상됩니다.");
                case "INDUSTRY" -> outlook.append("AI 산업 재편이 계속될 것이며, 인수합병과 전략적 파트너십이 " +
                        "더욱 활발해질 것으로 전망됩니다.");
                default -> outlook.append("해당 분야의 지속적인 발전과 함께 새로운 혁신이 기대됩니다.");
            }

            outlook.append("\n\n");
        }

        // 종합 전망
        outlook.append("AI 기술은 계속해서 빠르게 진화하고 있으며, 특히 실용적 응용과 산업 적용이 가속화되고 있습니다. " +
                "향후 1-2주간 주요 기업들의 제품 발표와 학계의 연구 성과 발표가 예정되어 있어, " +
                "AI 생태계 전반의 역동적인 변화가 계속될 것으로 예상됩니다.");

        return outlook.toString();
    }
}
