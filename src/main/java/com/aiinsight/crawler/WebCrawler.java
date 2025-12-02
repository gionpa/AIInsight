package com.aiinsight.crawler;

import com.aiinsight.domain.crawl.CrawlTarget;
import com.aiinsight.domain.crawl.SelectorConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebCrawler {

    private final CrawlerConfig crawlerConfig;
    private final ObjectMapper objectMapper;

    public CrawlResult crawl(CrawlTarget target) {
        long startTime = System.currentTimeMillis();
        List<CrawlResult.ArticleData> articles = new ArrayList<>();

        try {
            SelectorConfig config = parseSelectorConfig(target.getSelectorConfig());
            if (config == null) {
                return CrawlResult.builder()
                        .success(false)
                        .errorMessage("선택자 설정이 올바르지 않습니다")
                        .durationMs(System.currentTimeMillis() - startTime)
                        .build();
            }

            // 메인 페이지 크롤링
            Document doc = fetchDocument(target.getUrl());
            articles.addAll(extractArticles(doc, config, target.getUrl()));

            // 페이지네이션 처리
            if (config.getPagination() != null && Boolean.TRUE.equals(config.getPagination().getEnabled())) {
                articles.addAll(crawlPaginatedPages(target.getUrl(), config));
            }

            return CrawlResult.builder()
                    .success(true)
                    .articles(articles)
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();

        } catch (IOException e) {
            log.error("크롤링 실패: {} - {}", target.getName(), e.getMessage());
            return CrawlResult.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();
        } catch (Exception e) {
            log.error("크롤링 중 예상치 못한 오류: {} - {}", target.getName(), e.getMessage(), e);
            return CrawlResult.builder()
                    .success(false)
                    .errorMessage("예상치 못한 오류: " + e.getMessage())
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    /**
     * 웹 페이지 문서를 가져옵니다.
     * - 랜덤 User-Agent 사용 (봇 감지 회피)
     * - 브라우저와 동일한 HTTP 헤더 설정
     * - 403/429 에러 시 지수 백오프로 재시도
     */
    private Document fetchDocument(String url) throws IOException {
        int maxRetries = crawlerConfig.getRetryCount();
        int retryDelay = crawlerConfig.getRetryDelayMs();
        IOException lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return Jsoup.connect(url)
                        .userAgent(crawlerConfig.getRandomUserAgent())  // 랜덤 User-Agent
                        .timeout(crawlerConfig.getTimeout())
                        .followRedirects(true)
                        // 브라우저처럼 보이기 위한 필수 헤더
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
                        .header("Accept-Language", "en-US,en;q=0.9,ko;q=0.8")
                        .header("Accept-Encoding", "gzip, deflate, br")
                        .header("Connection", "keep-alive")
                        .header("Upgrade-Insecure-Requests", "1")
                        .header("sec-ch-ua", "\"Not_A Brand\";v=\"8\", \"Chromium\";v=\"120\", \"Google Chrome\";v=\"120\"")
                        .header("sec-ch-ua-mobile", "?0")
                        .header("sec-ch-ua-platform", "\"macOS\"")
                        .header("Sec-Fetch-Dest", "document")
                        .header("Sec-Fetch-Mode", "navigate")
                        .header("Sec-Fetch-Site", "none")
                        .header("Sec-Fetch-User", "?1")
                        .header("Cache-Control", "max-age=0")
                        .referrer("https://www.google.com/")  // 구글에서 온 것처럼
                        .get();
            } catch (org.jsoup.HttpStatusException e) {
                lastException = e;
                int statusCode = e.getStatusCode();

                // 403 (Forbidden) 또는 429 (Too Many Requests)는 재시도
                if ((statusCode == 403 || statusCode == 429) && attempt < maxRetries) {
                    // 지수 백오프: 2초, 4초, 8초...
                    int waitTime = retryDelay * (int) Math.pow(2, attempt - 1);
                    log.warn("HTTP {} 에러 발생, {}ms 후 재시도 ({}/{}): {}",
                            statusCode, waitTime, attempt, maxRetries, url);
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw e;
                    }
                } else {
                    // 다른 에러이거나 마지막 시도면 바로 throw
                    throw e;
                }
            } catch (IOException e) {
                lastException = e;
                if (attempt < maxRetries) {
                    int waitTime = retryDelay * (int) Math.pow(2, attempt - 1);
                    log.warn("IO 에러 발생, {}ms 후 재시도 ({}/{}): {} - {}",
                            waitTime, attempt, maxRetries, url, e.getMessage());
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw e;
                    }
                } else {
                    throw e;
                }
            }
        }

        // 모든 재시도 실패
        throw lastException != null ? lastException : new IOException("알 수 없는 오류로 크롤링 실패: " + url);
    }

    private List<CrawlResult.ArticleData> extractArticles(Document doc, SelectorConfig config, String baseUrl) {
        List<CrawlResult.ArticleData> articles = new ArrayList<>();

        Elements articleElements;
        if (config.getArticleListSelector() != null && config.getArticleItemSelector() != null) {
            Element listContainer = doc.selectFirst(config.getArticleListSelector());
            if (listContainer == null) {
                log.warn("기사 목록 컨테이너를 찾을 수 없습니다: {}", config.getArticleListSelector());
                return articles;
            }
            articleElements = listContainer.select(config.getArticleItemSelector());
        } else if (config.getArticleItemSelector() != null) {
            articleElements = doc.select(config.getArticleItemSelector());
            log.debug("선택자 '{}' 로 {} 개의 요소를 찾았습니다",
                    config.getArticleItemSelector(), articleElements.size());
        } else {
            log.warn("기사 아이템 선택자가 설정되지 않았습니다");
            return articles;
        }

        for (Element article : articleElements) {
            try {
                CrawlResult.ArticleData data = extractArticleData(article, config, baseUrl);
                if (data != null && data.getTitle() != null && data.getUrl() != null) {
                    articles.add(data);
                }
            } catch (Exception e) {
                log.warn("기사 데이터 추출 실패: {}", e.getMessage());
            }
        }

        return articles;
    }

    private CrawlResult.ArticleData extractArticleData(Element article, SelectorConfig config, String baseUrl) {
        // 제목 추출
        String title = null;
        if (config.getTitleSelector() != null) {
            Element titleEl = article.selectFirst(config.getTitleSelector());
            if (titleEl != null) {
                title = titleEl.text().trim();
            }
        }

        // 링크 추출
        String url = null;
        if (config.getLinkSelector() != null) {
            Element linkEl = article.selectFirst(config.getLinkSelector());
            if (linkEl != null) {
                url = linkEl.attr("abs:href");
                if (url.isEmpty()) {
                    url = linkEl.attr("href");
                    if (!url.startsWith("http")) {
                        url = resolveUrl(baseUrl, url);
                    }
                }
            }
        }

        // 본문 추출 (목록 페이지에서는 보통 없음)
        String content = null;
        if (config.getContentSelector() != null) {
            Element contentEl = article.selectFirst(config.getContentSelector());
            if (contentEl != null) {
                content = contentEl.text().trim();
            }
        }

        // 작성자 추출
        String author = null;
        if (config.getAuthorSelector() != null) {
            Element authorEl = article.selectFirst(config.getAuthorSelector());
            if (authorEl != null) {
                author = authorEl.text().trim();
            }
        }

        // 날짜 추출
        LocalDateTime publishedAt = null;
        if (config.getDateSelector() != null) {
            Element dateEl = article.selectFirst(config.getDateSelector());
            if (dateEl != null) {
                publishedAt = parseDate(dateEl.text().trim(), config.getDateFormat());
            }
        }

        // 썸네일 추출
        String thumbnailUrl = null;
        if (config.getThumbnailSelector() != null) {
            Element thumbEl = article.selectFirst(config.getThumbnailSelector());
            if (thumbEl != null) {
                thumbnailUrl = thumbEl.attr("abs:src");
                if (thumbnailUrl.isEmpty()) {
                    thumbnailUrl = thumbEl.attr("src");
                }
            }
        }

        return CrawlResult.ArticleData.builder()
                .url(url)
                .title(title)
                .content(content)
                .author(author)
                .publishedAt(publishedAt)
                .thumbnailUrl(thumbnailUrl)
                .build();
    }

    public CrawlResult.ArticleData fetchArticleDetail(String url, SelectorConfig config) {
        try {
            Document doc = fetchDocument(url);

            String title = null;
            if (config.getTitleSelector() != null) {
                Element titleEl = doc.selectFirst(config.getTitleSelector());
                if (titleEl != null) {
                    title = titleEl.text().trim();
                }
            }

            String content = null;
            if (config.getContentSelector() != null) {
                Element contentEl = doc.selectFirst(config.getContentSelector());
                if (contentEl != null) {
                    content = contentEl.text().trim();
                }
            }

            String author = null;
            if (config.getAuthorSelector() != null) {
                Element authorEl = doc.selectFirst(config.getAuthorSelector());
                if (authorEl != null) {
                    author = authorEl.text().trim();
                }
            }

            LocalDateTime publishedAt = null;
            if (config.getDateSelector() != null) {
                Element dateEl = doc.selectFirst(config.getDateSelector());
                if (dateEl != null) {
                    publishedAt = parseDate(dateEl.text().trim(), config.getDateFormat());
                }
            }

            return CrawlResult.ArticleData.builder()
                    .url(url)
                    .title(title)
                    .content(content)
                    .author(author)
                    .publishedAt(publishedAt)
                    .build();

        } catch (IOException e) {
            log.error("기사 상세 페이지 크롤링 실패: {} - {}", url, e.getMessage());
            return null;
        }
    }

    private List<CrawlResult.ArticleData> crawlPaginatedPages(String baseUrl, SelectorConfig config) {
        List<CrawlResult.ArticleData> allArticles = new ArrayList<>();
        SelectorConfig.PaginationConfig pagination = config.getPagination();

        int maxPages = pagination.getMaxPages() != null ? pagination.getMaxPages() : 5;

        for (int page = 2; page <= maxPages; page++) {
            try {
                Thread.sleep(crawlerConfig.getDelayBetweenRequests());

                String pageUrl = buildPageUrl(baseUrl, pagination.getPageParamName(), page);
                Document doc = fetchDocument(pageUrl);
                List<CrawlResult.ArticleData> pageArticles = extractArticles(doc, config, baseUrl);

                if (pageArticles.isEmpty()) {
                    break; // 더 이상 기사가 없으면 중단
                }

                allArticles.addAll(pageArticles);
                log.debug("페이지 {} 크롤링 완료: {} 개 기사", page, pageArticles.size());

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (IOException e) {
                log.warn("페이지 {} 크롤링 실패: {}", page, e.getMessage());
                break;
            }
        }

        return allArticles;
    }

    private String buildPageUrl(String baseUrl, String pageParam, int page) {
        if (baseUrl.contains("?")) {
            return baseUrl + "&" + pageParam + "=" + page;
        } else {
            return baseUrl + "?" + pageParam + "=" + page;
        }
    }

    private String resolveUrl(String baseUrl, String relativeUrl) {
        try {
            java.net.URI base = new java.net.URI(baseUrl);
            java.net.URI resolved = base.resolve(relativeUrl);
            return resolved.toString();
        } catch (Exception e) {
            return relativeUrl;
        }
    }

    private LocalDateTime parseDate(String dateStr, String format) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }

        // 여러 포맷 시도
        List<String> formats = new ArrayList<>();
        if (format != null && !format.isEmpty()) {
            formats.add(format);
        }
        formats.add("yyyy-MM-dd HH:mm:ss");
        formats.add("yyyy-MM-dd HH:mm");
        formats.add("yyyy-MM-dd");
        formats.add("yyyy.MM.dd HH:mm");
        formats.add("yyyy.MM.dd");
        formats.add("MMM dd, yyyy");
        formats.add("dd MMM yyyy");

        for (String fmt : formats) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(fmt, Locale.ENGLISH);
                if (fmt.contains("HH:mm")) {
                    return LocalDateTime.parse(dateStr, formatter);
                } else {
                    return java.time.LocalDate.parse(dateStr, formatter).atStartOfDay();
                }
            } catch (DateTimeParseException ignored) {
            }
        }

        log.warn("날짜 파싱 실패: {}", dateStr);
        return null;
    }

    private SelectorConfig parseSelectorConfig(String configJson) {
        if (configJson == null || configJson.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(configJson, SelectorConfig.class);
        } catch (Exception e) {
            log.error("선택자 설정 파싱 실패: {}", e.getMessage());
            return null;
        }
    }
}
