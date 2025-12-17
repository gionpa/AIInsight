import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class TestJsoup {
    public static void main(String[] args) {
        String url = "https://openai.com/index/new-chatgpt-images-is-here/";

        try {
            System.out.println("URL 크롤링 시작: " + url);

            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .timeout(10000)
                    .followRedirects(true)
                    .get();

            // 제목 추출
            String title = doc.select("meta[property=og:title]").attr("content");
            if (title.isEmpty()) {
                title = doc.select("meta[name=twitter:title]").attr("content");
            }
            if (title.isEmpty()) {
                title = doc.title();
            }
            System.out.println("\n제목: " + title);

            // 본문 추출
            String content = "";

            Element articleElem = doc.selectFirst("article");
            if (articleElem != null) {
                content = articleElem.text();
                System.out.println("\narticle 태그에서 본문 추출: " + content.length() + " chars");
            }

            if (content.isEmpty()) {
                Element mainElem = doc.selectFirst("main");
                if (mainElem != null) {
                    content = mainElem.text();
                    System.out.println("\nmain 태그에서 본문 추출: " + content.length() + " chars");
                }
            }

            if (content.isEmpty()) {
                Elements contentElems = doc.select(
                    ".article-content, .post-content, .entry-content, " +
                    ".content, .article-body, .post-body, .story-body, " +
                    "[class*=article], [class*=content], [class*=body], " +
                    "[id*=article], [id*=content], [id*=body]"
                );
                if (!contentElems.isEmpty()) {
                    content = contentElems.stream()
                            .map(Element::text)
                            .max((a, b) -> Integer.compare(a.length(), b.length()))
                            .orElse("");
                    System.out.println("\n클래스/ID 선택자에서 본문 추출: " + content.length() + " chars");
                }
            }

            if (content.length() > 0) {
                System.out.println("\n본문 미리보기 (첫 500자):");
                System.out.println(content.substring(0, Math.min(500, content.length())));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
