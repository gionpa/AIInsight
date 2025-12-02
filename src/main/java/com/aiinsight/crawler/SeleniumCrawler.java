package com.aiinsight.crawler;

import com.aiinsight.domain.crawl.CrawlTarget;
import com.aiinsight.domain.crawl.SelectorConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bonigarcia.wdm.WebDriverManager;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
@Slf4j
public class SeleniumCrawler {

    private final CrawlerConfig crawlerConfig;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        log.info("SeleniumCrawler 초기화 - ChromeDriver 설정 중...");
        WebDriverManager.chromedriver().setup();
        log.info("ChromeDriver 설정 완료");
    }

    public CrawlResult crawl(CrawlTarget target) {
        long startTime = System.currentTimeMillis();
        List<CrawlResult.ArticleData> articles = new ArrayList<>();
        WebDriver driver = null;

        try {
            SelectorConfig config = parseSelectorConfig(target.getSelectorConfig());
            if (config == null) {
                return CrawlResult.builder()
                        .success(false)
                        .errorMessage("선택자 설정이 올바르지 않습니다")
                        .durationMs(System.currentTimeMillis() - startTime)
                        .build();
            }

            driver = createDriver();
            log.info("Selenium 크롤링 시작: {} ({})", target.getName(), target.getUrl());

            driver.get(target.getUrl());

            // JavaScript 렌더링 대기
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            waitForPageLoad(driver, wait, config);

            // 스크롤하여 동적 콘텐츠 로드
            scrollToLoadContent(driver);

            // 기사 추출
            articles.addAll(extractArticles(driver, config, target.getUrl()));

            return CrawlResult.builder()
                    .success(true)
                    .articles(articles)
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();

        } catch (Exception e) {
            log.error("Selenium 크롤링 실패: {} - {}", target.getName(), e.getMessage(), e);
            return CrawlResult.builder()
                    .success(false)
                    .errorMessage("Selenium 크롤링 실패: " + e.getMessage())
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();
        } finally {
            if (driver != null) {
                try {
                    driver.quit();
                } catch (Exception e) {
                    log.warn("WebDriver 종료 중 오류: {}", e.getMessage());
                }
            }
        }
    }

    private WebDriver createDriver() {
        ChromeOptions options = new ChromeOptions();

        // Docker/Railway 환경에서 Chrome 바이너리 경로 설정
        String chromeBinary = System.getenv("CHROME_BIN");
        if (chromeBinary != null && !chromeBinary.isEmpty()) {
            log.info("Chrome 바이너리 경로 설정: {}", chromeBinary);
            options.setBinary(chromeBinary);
        }

        // Headless 모드 설정
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");

        // Docker 환경 추가 설정
        options.addArguments("--disable-setuid-sandbox");
        options.addArguments("--single-process");
        options.addArguments("--remote-debugging-port=9222");

        // 봇 감지 우회를 위한 설정
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setExperimentalOption("excludeSwitches", List.of("enable-automation"));
        options.setExperimentalOption("useAutomationExtension", false);

        // User-Agent 설정
        options.addArguments("user-agent=" + crawlerConfig.getRandomUserAgent());

        // 이미지 로딩 비활성화 (성능 향상)
        options.addArguments("--blink-settings=imagesEnabled=false");

        // 메모리 사용량 최적화
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-plugins");
        options.addArguments("--disable-infobars");

        return new ChromeDriver(options);
    }

    private void waitForPageLoad(WebDriver driver, WebDriverWait wait, SelectorConfig config) {
        try {
            // 페이지 기본 로딩 대기
            wait.until(webDriver ->
                    ((JavascriptExecutor) webDriver).executeScript("return document.readyState").equals("complete")
            );

            // 기사 아이템이 로드될 때까지 대기
            if (config.getArticleItemSelector() != null) {
                try {
                    wait.until(ExpectedConditions.presenceOfElementLocated(
                            By.cssSelector(config.getArticleItemSelector())
                    ));
                } catch (TimeoutException e) {
                    log.warn("기사 아이템 선택자 대기 타임아웃: {}", config.getArticleItemSelector());
                }
            }

            // 추가 렌더링 시간 대기
            Thread.sleep(2000);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.warn("페이지 로드 대기 중 오류: {}", e.getMessage());
        }
    }

    private void scrollToLoadContent(WebDriver driver) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;

            // 페이지 하단까지 스크롤 (동적 콘텐츠 로드)
            long lastHeight = (Long) js.executeScript("return document.body.scrollHeight");

            for (int i = 0; i < 3; i++) {
                js.executeScript("window.scrollTo(0, document.body.scrollHeight)");
                Thread.sleep(1500);

                long newHeight = (Long) js.executeScript("return document.body.scrollHeight");
                if (newHeight == lastHeight) {
                    break;
                }
                lastHeight = newHeight;
            }

            // 맨 위로 스크롤
            js.executeScript("window.scrollTo(0, 0)");
            Thread.sleep(500);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.warn("스크롤 중 오류: {}", e.getMessage());
        }
    }

    private List<CrawlResult.ArticleData> extractArticles(WebDriver driver, SelectorConfig config, String baseUrl) {
        List<CrawlResult.ArticleData> articles = new ArrayList<>();

        try {
            List<WebElement> articleElements;

            if (config.getArticleListSelector() != null && config.getArticleItemSelector() != null) {
                WebElement listContainer = driver.findElement(By.cssSelector(config.getArticleListSelector()));
                articleElements = listContainer.findElements(By.cssSelector(config.getArticleItemSelector()));
            } else if (config.getArticleItemSelector() != null) {
                articleElements = driver.findElements(By.cssSelector(config.getArticleItemSelector()));
                log.info("Selenium: 선택자 '{}' 로 {} 개의 요소를 찾았습니다",
                        config.getArticleItemSelector(), articleElements.size());
            } else {
                log.warn("기사 아이템 선택자가 설정되지 않았습니다");
                return articles;
            }

            for (WebElement article : articleElements) {
                try {
                    CrawlResult.ArticleData data = extractArticleData(article, config, baseUrl);
                    if (data != null && data.getTitle() != null && data.getUrl() != null) {
                        articles.add(data);
                    }
                } catch (StaleElementReferenceException e) {
                    log.warn("요소가 DOM에서 분리되었습니다. 건너뜁니다.");
                } catch (Exception e) {
                    log.warn("기사 데이터 추출 실패: {}", e.getMessage());
                }
            }

        } catch (NoSuchElementException e) {
            log.warn("기사 목록을 찾을 수 없습니다: {}", e.getMessage());
        } catch (Exception e) {
            log.error("기사 추출 중 오류: {}", e.getMessage());
        }

        return articles;
    }

    private CrawlResult.ArticleData extractArticleData(WebElement article, SelectorConfig config, String baseUrl) {
        // 제목 추출
        String title = null;
        if (config.getTitleSelector() != null) {
            try {
                WebElement titleEl = article.findElement(By.cssSelector(config.getTitleSelector()));
                title = titleEl.getText().trim();
            } catch (NoSuchElementException ignored) {
            }
        }
        // 제목이 없으면 기사 요소 자체의 텍스트 사용
        if ((title == null || title.isEmpty()) && article.getTagName().equalsIgnoreCase("a")) {
            title = article.getText().trim();
            // 너무 긴 텍스트는 자르기 (첫 번째 줄만)
            if (title.contains("\n")) {
                title = title.split("\n")[0].trim();
            }
        }

        // 링크 추출
        String url = null;
        // 기사 요소 자체가 앵커인 경우 직접 href 추출
        if (article.getTagName().equalsIgnoreCase("a")) {
            url = article.getAttribute("href");
            if (url != null && !url.startsWith("http")) {
                url = resolveUrl(baseUrl, url);
            }
        } else if (config.getLinkSelector() != null) {
            try {
                WebElement linkEl = article.findElement(By.cssSelector(config.getLinkSelector()));
                url = linkEl.getAttribute("href");
                if (url != null && !url.startsWith("http")) {
                    url = resolveUrl(baseUrl, url);
                }
            } catch (NoSuchElementException ignored) {
            }
        }

        // 본문 추출 (목록 페이지에서는 보통 없음)
        String content = null;
        if (config.getContentSelector() != null) {
            try {
                WebElement contentEl = article.findElement(By.cssSelector(config.getContentSelector()));
                content = contentEl.getText().trim();
            } catch (NoSuchElementException ignored) {
            }
        }

        // 작성자 추출
        String author = null;
        if (config.getAuthorSelector() != null) {
            try {
                WebElement authorEl = article.findElement(By.cssSelector(config.getAuthorSelector()));
                author = authorEl.getText().trim();
            } catch (NoSuchElementException ignored) {
            }
        }

        // 날짜 추출
        LocalDateTime publishedAt = null;
        if (config.getDateSelector() != null) {
            try {
                WebElement dateEl = article.findElement(By.cssSelector(config.getDateSelector()));
                publishedAt = parseDate(dateEl.getText().trim(), config.getDateFormat());
            } catch (NoSuchElementException ignored) {
            }
        }

        // 썸네일 추출
        String thumbnailUrl = null;
        if (config.getThumbnailSelector() != null) {
            try {
                WebElement thumbEl = article.findElement(By.cssSelector(config.getThumbnailSelector()));
                thumbnailUrl = thumbEl.getAttribute("src");
            } catch (NoSuchElementException ignored) {
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
