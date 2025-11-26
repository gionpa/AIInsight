-- AI 뉴스 크롤링 타겟 초기 데이터

INSERT INTO crawl_target (name, url, description, selector_config, cron_expression, enabled, crawl_type, created_at, updated_at)
VALUES
-- LLM 기업 공식 블로그
('OpenAI Blog', 'https://openai.com/blog', 'OpenAI 공식 블로그 - GPT, DALL-E, Sora 등 최신 소식',
 '{"articleItemSelector": "li[class*=''card'']", "titleSelector": "h3", "linkSelector": "a", "dateSelector": "time"}',
 '0 0 */4 * * *', true, 'STATIC', NOW(), NOW()),

('Anthropic News', 'https://www.anthropic.com/news', 'Anthropic 공식 뉴스 - Claude AI 관련 소식',
 '{"articleItemSelector": "a[class*=''PostCard'']", "titleSelector": "h3", "linkSelector": "a"}',
 '0 0 */4 * * *', true, 'STATIC', NOW(), NOW()),

('Google AI Blog', 'https://blog.google/technology/ai/', 'Google AI 공식 블로그 - Gemini 등',
 '{"articleItemSelector": "article", "titleSelector": "h3", "linkSelector": "a", "dateSelector": "time"}',
 '0 0 */4 * * *', true, 'STATIC', NOW(), NOW()),

('Meta AI Blog', 'https://ai.meta.com/blog/', 'Meta AI 공식 블로그 - Llama, Research 등',
 '{"articleItemSelector": "div[class*=''BlogCard'']", "titleSelector": "h3", "linkSelector": "a"}',
 '0 0 */4 * * *', true, 'STATIC', NOW(), NOW()),

('Microsoft AI Blog', 'https://blogs.microsoft.com/ai/', 'Microsoft AI 공식 블로그 - Copilot, Azure AI 등',
 '{"articleItemSelector": "article", "titleSelector": "h2", "linkSelector": "a", "dateSelector": "time"}',
 '0 0 */4 * * *', true, 'STATIC', NOW(), NOW()),

-- AI 전문 미디어
('TechCrunch AI', 'https://techcrunch.com/category/artificial-intelligence/', 'TechCrunch AI 섹션',
 '{"articleItemSelector": "article", "titleSelector": "h2 a", "linkSelector": "h2 a", "dateSelector": "time"}',
 '0 0 */2 * * *', true, 'STATIC', NOW(), NOW()),

('VentureBeat AI', 'https://venturebeat.com/category/ai/', 'VentureBeat AI 섹션',
 '{"articleItemSelector": "article", "titleSelector": "h2 a", "linkSelector": "h2 a", "dateSelector": "time"}',
 '0 0 */2 * * *', true, 'STATIC', NOW(), NOW()),

('The Verge AI', 'https://www.theverge.com/ai-artificial-intelligence', 'The Verge AI 섹션',
 '{"articleItemSelector": "div[class*=''duet--content-cards'']", "titleSelector": "h2 a", "linkSelector": "a", "dateSelector": "time"}',
 '0 0 */2 * * *', true, 'STATIC', NOW(), NOW()),

('Wired AI', 'https://www.wired.com/tag/artificial-intelligence/', 'Wired AI 섹션',
 '{"articleItemSelector": "div[class*=''SummaryItem'']", "titleSelector": "h3 a", "linkSelector": "a", "dateSelector": "time"}',
 '0 0 */3 * * *', true, 'STATIC', NOW(), NOW()),

('MIT Technology Review AI', 'https://www.technologyreview.com/topic/artificial-intelligence/', 'MIT Technology Review AI 섹션',
 '{"articleItemSelector": "article", "titleSelector": "h3 a", "linkSelector": "a", "dateSelector": "time"}',
 '0 0 */3 * * *', true, 'STATIC', NOW(), NOW()),

-- AI 연구 및 논문
('arXiv AI', 'https://arxiv.org/list/cs.AI/recent', 'arXiv AI 최신 논문',
 '{"articleItemSelector": "dt", "titleSelector": "div.list-title", "linkSelector": "a[title=''Abstract'']"}',
 '0 0 */6 * * *', true, 'STATIC', NOW(), NOW()),

('Hugging Face Blog', 'https://huggingface.co/blog', 'Hugging Face 공식 블로그 - 오픈소스 ML',
 '{"articleItemSelector": "article", "titleSelector": "h2", "linkSelector": "a"}',
 '0 0 */4 * * *', true, 'STATIC', NOW(), NOW()),

-- 한국 AI 뉴스
('AI타임스', 'https://www.aitimes.com/news/articleList.html?sc_section_code=S1N1', 'AI타임스 국내 AI 뉴스',
 '{"articleItemSelector": "div.list-block", "titleSelector": "h4.titles a", "linkSelector": "a", "dateSelector": "span.byline"}',
 '0 0 */2 * * *', true, 'STATIC', NOW(), NOW()),

('인공지능신문', 'http://www.aitimes.kr/news/articleList.html', '인공지능신문 국내 AI 뉴스',
 '{"articleItemSelector": "div.list-block", "titleSelector": "h4.titles a", "linkSelector": "a"}',
 '0 0 */2 * * *', true, 'STATIC', NOW(), NOW());
