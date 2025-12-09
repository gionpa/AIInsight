import { apiClient } from './client';
import type {
  CrawlTarget,
  CreateCrawlTargetRequest,
  UpdateCrawlTargetRequest,
  NewsArticle,
  NewsArticleDetail,
  CrawlHistory,
  DashboardStats,
  Page,
  CrawlResult,
  ArticleCategory,
  DailyReport,
  Phase3DailyReport,
  CategoryReport,
  InterestTopic,
  CreateInterestTopicRequest,
  UpdateInterestTopicRequest,
  TopicReportResponse,
  AllTopicsReportResponse,
} from '../types';

// Dashboard
export const getDashboardStats = () =>
  apiClient.get<DashboardStats>('/dashboard/stats').then((res) => res.data);

// Crawl Targets
export const getCrawlTargets = (page = 0, size = 20) =>
  apiClient
    .get<Page<CrawlTarget>>('/crawl-targets', { params: { page, size } })
    .then((res) => res.data);

export const getAllCrawlTargets = () =>
  apiClient.get<CrawlTarget[]>('/crawl-targets/all').then((res) => res.data);

export const getCrawlTarget = (id: number) =>
  apiClient.get<CrawlTarget>(`/crawl-targets/${id}`).then((res) => res.data);

export const createCrawlTarget = (data: CreateCrawlTargetRequest) =>
  apiClient.post<CrawlTarget>('/crawl-targets', data).then((res) => res.data);

export const updateCrawlTarget = (id: number, data: UpdateCrawlTargetRequest) =>
  apiClient.put<CrawlTarget>(`/crawl-targets/${id}`, data).then((res) => res.data);

export const deleteCrawlTarget = (id: number) =>
  apiClient.delete(`/crawl-targets/${id}`);

export const toggleCrawlTarget = (id: number) =>
  apiClient.post<CrawlTarget>(`/crawl-targets/${id}/toggle`).then((res) => res.data);

// News Articles
export const getArticles = (page = 0, size = 20) =>
  apiClient
    .get<Page<NewsArticle>>('/articles', { params: { page, size } })
    .then((res) => res.data);

export const getArticle = (id: number) =>
  apiClient.get<NewsArticleDetail>(`/articles/${id}`).then((res) => res.data);

export const getNewArticles = (page = 0, size = 20) =>
  apiClient
    .get<Page<NewsArticle>>('/articles/new', { params: { page, size } })
    .then((res) => res.data);

export const getArticlesByCategory = (category: ArticleCategory, page = 0, size = 20) =>
  apiClient
    .get<Page<NewsArticle>>(`/articles/category/${category}`, { params: { page, size } })
    .then((res) => res.data);

export const getArticlesByTarget = (targetId: number, page = 0, size = 20) =>
  apiClient
    .get<Page<NewsArticle>>(`/articles/target/${targetId}`, { params: { page, size } })
    .then((res) => res.data);

export const searchArticles = (keyword: string, page = 0, size = 20) =>
  apiClient
    .get<Page<NewsArticle>>('/articles/search', { params: { keyword, page, size } })
    .then((res) => res.data);

export const getRelevantArticles = (minScore = 0.7, page = 0, size = 20) =>
  apiClient
    .get<Page<NewsArticle>>('/articles/relevant', { params: { minScore, page, size } })
    .then((res) => res.data);

export const getArticlesByImportance = (importance: 'HIGH' | 'MEDIUM' | 'LOW', page = 0, size = 20) =>
  apiClient
    .get<Page<NewsArticle>>(`/articles/importance/${importance}`, { params: { page, size } })
    .then((res) => res.data);

export const markArticleAsRead = (id: number) =>
  apiClient.post(`/articles/${id}/read`);

export const markArticlesAsRead = (ids: number[]) =>
  apiClient.post('/articles/read', ids);

export const deleteArticle = (id: number) =>
  apiClient.delete(`/articles/${id}`);

export const getNewArticlesCount = () =>
  apiClient.get<number>('/articles/count/new').then((res) => res.data);

// Crawl History
export const getCrawlHistoryByTarget = (targetId: number, page = 0, size = 20) =>
  apiClient
    .get<Page<CrawlHistory>>(`/crawl-history/target/${targetId}`, { params: { page, size } })
    .then((res) => res.data);

export const getTodayCrawlHistory = () =>
  apiClient.get<CrawlHistory[]>('/crawl-history/today').then((res) => res.data);

export const getAllCrawlHistory = (page = 0, size = 30) =>
  apiClient
    .get<Page<CrawlHistory>>('/crawl-history', { params: { page, size } })
    .then((res) => res.data);

export const deleteOldCrawlHistory = () =>
  apiClient.delete<{ deletedCount: number }>('/crawl-history/old').then((res) => res.data);

// Crawl Execution
export const executeCrawl = (targetId: number) =>
  apiClient.post<CrawlResult>(`/crawl/execute/${targetId}`).then((res) => res.data);

export const executeAllCrawls = () =>
  apiClient.post('/crawl/execute-all');

// Scheduler
export const refreshScheduler = () =>
  apiClient.post('/scheduler/refresh').then((res) => res.data);

export const getSchedulerStatus = () =>
  apiClient.get('/scheduler/status').then((res) => res.data);

// Reports (Phase 3: Updated endpoints)
export const getDailyReport = () =>
  apiClient.get<DailyReport>('/reports/latest').then((res) => res.data);

export const getPhase3DailyReport = () =>
  apiClient.get<Phase3DailyReport>('/reports/latest-phase3').then((res) => res.data);

export const getCategoryReport = () =>
  apiClient.get<CategoryReport[]>('/reports/by-category').then((res) => res.data);

export const getReportByCategory = (category: string) =>
  apiClient.get<CategoryReport>(`/reports/category/${category}`).then((res) => res.data);

export const generateTodayReport = () =>
  apiClient.post<Phase3DailyReport>('/reports/generate/today').then((res) => res.data);

// Interest Topics
export const getInterestTopics = () =>
  apiClient.get<InterestTopic[]>('/interest-topics').then((res) => res.data);

export const getActiveInterestTopics = () =>
  apiClient.get<InterestTopic[]>('/interest-topics/active').then((res) => res.data);

export const getInterestTopic = (id: number) =>
  apiClient.get<InterestTopic>(`/interest-topics/${id}`).then((res) => res.data);

export const createInterestTopic = (data: CreateInterestTopicRequest) =>
  apiClient.post<InterestTopic>('/interest-topics', data).then((res) => res.data);

export const updateInterestTopic = (id: number, data: UpdateInterestTopicRequest) =>
  apiClient.put<InterestTopic>(`/interest-topics/${id}`, data).then((res) => res.data);

export const deleteInterestTopic = (id: number) =>
  apiClient.delete(`/interest-topics/${id}`);

export const reorderInterestTopics = (topicIds: number[]) =>
  apiClient.post<InterestTopic[]>('/interest-topics/reorder', { topicIds }).then((res) => res.data);

export const getTopicReport = (topicId: number, limit = 10) =>
  apiClient.get<TopicReportResponse>(`/interest-topics/${topicId}/report`, { params: { limit } }).then((res) => res.data);

export const getAllTopicsReport = (articlesPerTopic = 5) =>
  apiClient.get<AllTopicsReportResponse>('/interest-topics/report', { params: { articlesPerTopic } }).then((res) => res.data);

export const initializeDefaultTopics = () =>
  apiClient.post<InterestTopic[]>('/interest-topics/initialize').then((res) => res.data);
