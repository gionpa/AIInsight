export interface CrawlTarget {
  id: number;
  name: string;
  url: string;
  description?: string;
  selectorConfig?: string;
  cronExpression: string;
  enabled: boolean;
  crawlType: 'STATIC' | 'DYNAMIC';
  lastCrawledAt?: string;
  lastStatus?: 'SUCCESS' | 'FAILED' | 'PARTIAL';
  createdAt: string;
  updatedAt: string;
}

export interface CreateCrawlTargetRequest {
  name: string;
  url: string;
  description?: string;
  selectorConfig?: string;
  cronExpression: string;
  crawlType?: 'STATIC' | 'DYNAMIC';
  enabled?: boolean;
}

export interface UpdateCrawlTargetRequest {
  name?: string;
  url?: string;
  description?: string;
  selectorConfig?: string;
  cronExpression?: string;
  crawlType?: 'STATIC' | 'DYNAMIC';
  enabled?: boolean;
}

export interface NewsArticle {
  id: number;
  targetId: number;
  targetName: string;
  originalUrl: string;
  title: string;
  titleKo?: string;
  summary?: string;
  author?: string;
  publishedAt?: string;
  relevanceScore?: number;
  category?: ArticleCategory;
  importance?: ArticleImportance;
  analysisStatus?: AnalysisStatus;
  isNew: boolean;
  isSummarized: boolean;
  thumbnailUrl?: string;
  crawledAt: string;
}

export interface NewsArticleDetail extends NewsArticle {
  content?: string;
  updatedAt: string;
}

export type AnalysisStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';

export type ArticleCategory =
  | 'LLM'
  | 'COMPUTER_VISION'
  | 'NLP'
  | 'ROBOTICS'
  | 'ML_OPS'
  | 'RESEARCH'
  | 'INDUSTRY'
  | 'STARTUP'
  | 'REGULATION'
  | 'TUTORIAL'
  | 'PRODUCT'
  | 'OTHER';

export type ArticleImportance = 'HIGH' | 'MEDIUM' | 'LOW';

export interface CrawlHistory {
  id: number;
  targetId: number;
  targetName: string;
  status: 'SUCCESS' | 'FAILED' | 'PARTIAL';
  articlesFound: number;
  articlesNew: number;
  durationMs: number;
  errorMessage?: string;
  executedAt: string;
}

export interface DashboardStats {
  totalTargets: number;
  activeTargets: number;
  totalArticles: number;
  newArticles: number;
  todayCrawled: number;
  successfulCrawls: number;
  failedCrawls: number;
  categoryDistribution: Record<ArticleCategory, number>;
  recentCrawls: RecentCrawl[];
}

export interface RecentCrawl {
  targetId: number;
  targetName: string;
  status: string;
  articlesNew: number;
  executedAt: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface CrawlResult {
  success: boolean;
  articlesFound: number;
  durationMs: number;
  errorMessage?: string;
}

export interface DailyReport {
  generatedAt: string;
  period: string;
  totalHighImportanceArticles: number;
  categoryDistribution: Record<string, number>;
  articles: ArticleSummary[];
  executiveSummary: string;
}

// Phase 3: 임베딩 기반 토픽 클러스터링 리포트
export interface Phase3DailyReport {
  id: number;
  reportDate: string;  // yyyy-MM-dd
  executiveSummary: string;
  keyTrends: KeyTrend[];
  topicSummaries: TopicSummary[];
  totalArticles: number;
  highImportanceArticles: number;
  status: 'PENDING' | 'COMPLETED' | 'FAILED';
  generatedAt: string;  // yyyy-MM-dd'T'HH:mm:ss
}

export interface KeyTrend {
  keyword: string;
  frequency: number;
}

export interface TopicSummary {
  topic: string;
  articleCount: number;
  representativeTitles: string[];
}

export interface ArticleSummary {
  id: number;
  title: string;           // 한글 제목 (titleKo 우선)
  originalTitle?: string;  // 원문 제목
  summary?: string;
  category?: string;
  relevanceScore?: number;
  sourceName?: string;
  originalUrl: string;
  crawledAt: string;
}

export interface CategoryReport {
  category: string;
  categoryDisplayName: string;
  articleCount: number;
  articles: ArticleSummary[];
}

// 관심 주제 관련 타입
export interface InterestTopic {
  id: number;
  name: string;
  description?: string;
  keywords: string;
  displayOrder: number;
  isActive: boolean;
  createdAt: string;
  updatedAt?: string;
}

export interface CreateInterestTopicRequest {
  name: string;
  description?: string;
  keywords: string;
}

export interface UpdateInterestTopicRequest {
  name?: string;
  description?: string;
  keywords?: string;
  isActive?: boolean;
}

export interface TopicReportResponse {
  topicId: number;
  topicName: string;
  description?: string;
  keywords: string;
  totalArticles: number;
  highImportanceCount: number;
  articles: NewsArticle[];
}

export interface AllTopicsReportResponse {
  generatedAt: string;
  totalTopics: number;
  totalArticles: number;
  topics: TopicReportResponse[];
}
