-- Railway PostgreSQL Schema for AIInsight
-- Created: 2025-11-26

-- Drop tables if exist (for clean setup)
DROP TABLE IF EXISTS crawl_history CASCADE;
DROP TABLE IF EXISTS news_article CASCADE;
DROP TABLE IF EXISTS crawl_target CASCADE;

-- 1. Crawl Target Table
CREATE TABLE crawl_target (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    url VARCHAR(2048) NOT NULL,
    description TEXT,
    selector_config TEXT,
    cron_expression VARCHAR(100) NOT NULL DEFAULT '0 0 * * * ?',
    crawl_type VARCHAR(20) NOT NULL DEFAULT 'STATIC',
    enabled BOOLEAN NOT NULL DEFAULT true,
    last_crawled_at TIMESTAMP,
    last_status VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. News Article Table
CREATE TABLE news_article (
    id BIGSERIAL PRIMARY KEY,
    target_id BIGINT NOT NULL REFERENCES crawl_target(id) ON DELETE CASCADE,
    original_url VARCHAR(2048) NOT NULL,
    title VARCHAR(500) NOT NULL,
    title_ko VARCHAR(500),  -- 한글 번역 제목
    content TEXT,
    summary TEXT,
    author VARCHAR(255),
    published_at TIMESTAMP,
    relevance_score DOUBLE PRECISION,
    category VARCHAR(50),
    importance VARCHAR(10),
    is_new BOOLEAN NOT NULL DEFAULT true,
    is_summarized BOOLEAN NOT NULL DEFAULT false,
    thumbnail_url VARCHAR(1024),
    content_hash VARCHAR(64) NOT NULL UNIQUE,
    crawled_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. Crawl History Table
CREATE TABLE crawl_history (
    id BIGSERIAL PRIMARY KEY,
    target_id BIGINT NOT NULL REFERENCES crawl_target(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL,
    articles_found INTEGER NOT NULL DEFAULT 0,
    articles_new INTEGER NOT NULL DEFAULT 0,
    duration_ms BIGINT NOT NULL DEFAULT 0,
    error_message TEXT,
    executed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better performance
CREATE INDEX idx_news_article_target_id ON news_article(target_id);
CREATE INDEX idx_news_article_crawled_at ON news_article(crawled_at DESC);
CREATE INDEX idx_news_article_category ON news_article(category);
CREATE INDEX idx_news_article_importance ON news_article(importance);
CREATE INDEX idx_news_article_is_new ON news_article(is_new);
CREATE INDEX idx_news_article_is_summarized ON news_article(is_summarized);
CREATE INDEX idx_crawl_history_target_id ON crawl_history(target_id);
CREATE INDEX idx_crawl_history_executed_at ON crawl_history(executed_at DESC);

-- Insert seed data
INSERT INTO crawl_target (name, url, description, cron_expression, crawl_type, enabled) VALUES
('OpenAI Blog', 'https://openai.com/news/', 'Official OpenAI news and updates', '0 0 */6 * * ?', 'DYNAMIC', true),
('Anthropic Blog', 'https://www.anthropic.com/news', 'Anthropic AI research and announcements', '0 0 */6 * * ?', 'DYNAMIC', true),
('Google AI Blog', 'https://ai.googleblog.com/', 'Google AI research and updates', '0 0 */12 * * ?', 'DYNAMIC', true),
('MIT Technology Review AI', 'https://www.technologyreview.com/topic/artificial-intelligence/', 'MIT Tech Review AI coverage', '0 0 */12 * * ?', 'DYNAMIC', true),
('arXiv AI Papers', 'https://arxiv.org/list/cs.AI/recent', 'Recent AI papers on arXiv', '0 0 0 * * ?', 'STATIC', true),
('Hugging Face Blog', 'https://huggingface.co/blog', 'Hugging Face ML blog', '0 0 */12 * * ?', 'DYNAMIC', true),
('DeepMind Blog', 'https://deepmind.google/discover/blog/', 'DeepMind research blog', '0 0 */12 * * ?', 'DYNAMIC', true),
('Meta AI Blog', 'https://ai.meta.com/blog/', 'Meta (Facebook) AI research', '0 0 */12 * * ?', 'DYNAMIC', true),
('Microsoft AI Blog', 'https://blogs.microsoft.com/ai/', 'Microsoft AI news and updates', '0 0 */12 * * ?', 'DYNAMIC', true),
('NVIDIA AI Blog', 'https://blogs.nvidia.com/blog/category/deep-learning/', 'NVIDIA deep learning blog', '0 0 */12 * * ?', 'DYNAMIC', true),
('AI News - The Verge', 'https://www.theverge.com/ai-artificial-intelligence', 'The Verge AI coverage', '0 0 */8 * * ?', 'DYNAMIC', true),
('VentureBeat AI', 'https://venturebeat.com/category/ai/', 'VentureBeat AI news', '0 0 */8 * * ?', 'DYNAMIC', true),
('TechCrunch AI', 'https://techcrunch.com/category/artificial-intelligence/', 'TechCrunch AI coverage', '0 0 */8 * * ?', 'DYNAMIC', true);

-- Success message
SELECT 'Database schema created successfully!' AS status,
       (SELECT COUNT(*) FROM crawl_target) AS crawl_targets_inserted;
