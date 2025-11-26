import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getDailyReport, getCategoryReport } from '../api';
import {
  FileText,
  AlertCircle,
  TrendingUp,
  ExternalLink,
  Clock,
  Tag,
  ChevronDown,
  ChevronUp,
} from 'lucide-react';
import type { CategoryReport, ArticleSummary } from '../types';

const CATEGORY_COLORS: Record<string, string> = {
  LLM: 'bg-purple-100 text-purple-800',
  COMPUTER_VISION: 'bg-blue-100 text-blue-800',
  NLP: 'bg-green-100 text-green-800',
  ROBOTICS: 'bg-orange-100 text-orange-800',
  ML_OPS: 'bg-yellow-100 text-yellow-800',
  RESEARCH: 'bg-indigo-100 text-indigo-800',
  INDUSTRY: 'bg-gray-100 text-gray-800',
  STARTUP: 'bg-pink-100 text-pink-800',
  REGULATION: 'bg-red-100 text-red-800',
  TUTORIAL: 'bg-teal-100 text-teal-800',
  PRODUCT: 'bg-cyan-100 text-cyan-800',
  OTHER: 'bg-slate-100 text-slate-800',
};

function ArticleCard({ article }: { article: ArticleSummary }) {
  const [expanded, setExpanded] = useState(false);

  return (
    <div className="bg-white rounded-lg border border-gray-200 p-4 hover:shadow-md transition-shadow">
      <div className="flex items-start justify-between gap-3">
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 mb-2">
            {article.category && (
              <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${CATEGORY_COLORS[article.category] || CATEGORY_COLORS.OTHER}`}>
                {article.category}
              </span>
            )}
            {article.relevanceScore && (
              <span className="flex items-center gap-1 text-xs text-gray-500">
                <TrendingUp className="w-3 h-3" />
                {(article.relevanceScore * 100).toFixed(0)}%
              </span>
            )}
          </div>
          <h3 className="font-semibold text-gray-900 mb-1 line-clamp-2">
            {article.title}
          </h3>
          {article.originalTitle && article.originalTitle !== article.title && (
            <p className="text-xs text-gray-400 mb-1 line-clamp-1 italic">
              {article.originalTitle}
            </p>
          )}
          <div className="flex items-center gap-3 text-xs text-gray-500 mb-2">
            {article.sourceName && (
              <span className="flex items-center gap-1">
                <Tag className="w-3 h-3" />
                {article.sourceName}
              </span>
            )}
            <span className="flex items-center gap-1">
              <Clock className="w-3 h-3" />
              {new Date(article.crawledAt).toLocaleString('ko-KR', {
                year: 'numeric',
                month: '2-digit',
                day: '2-digit',
                hour: '2-digit',
                minute: '2-digit'
              })}
            </span>
          </div>
        </div>
        <a
          href={article.originalUrl}
          target="_blank"
          rel="noopener noreferrer"
          className="flex-shrink-0 p-2 text-gray-400 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
        >
          <ExternalLink className="w-4 h-4" />
        </a>
      </div>

      {article.summary && (
        <div className="mt-3">
          <button
            onClick={() => setExpanded(!expanded)}
            className="flex items-center gap-1 text-sm text-blue-600 hover:text-blue-800"
          >
            {expanded ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
            {expanded ? '요약 접기' : '요약 보기'}
          </button>
          {expanded && (
            <p className="mt-2 text-sm text-gray-600 leading-relaxed bg-gray-50 p-3 rounded-lg">
              {article.summary}
            </p>
          )}
        </div>
      )}
    </div>
  );
}

function CategorySection({ category }: { category: CategoryReport }) {
  const [collapsed, setCollapsed] = useState(false);

  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
      <button
        onClick={() => setCollapsed(!collapsed)}
        className="w-full flex items-center justify-between p-4 bg-gray-50 hover:bg-gray-100 transition-colors"
      >
        <div className="flex items-center gap-3">
          <span className={`px-3 py-1 rounded-full text-sm font-medium ${CATEGORY_COLORS[category.category] || CATEGORY_COLORS.OTHER}`}>
            {category.categoryDisplayName}
          </span>
          <span className="text-sm text-gray-500">
            {category.articleCount}개 기사
          </span>
        </div>
        {collapsed ? <ChevronDown className="w-5 h-5 text-gray-400" /> : <ChevronUp className="w-5 h-5 text-gray-400" />}
      </button>
      {!collapsed && (
        <div className="p-4 space-y-3">
          {category.articles.map((article) => (
            <ArticleCard key={article.id} article={article} />
          ))}
        </div>
      )}
    </div>
  );
}

export default function Report() {
  const [viewMode, setViewMode] = useState<'daily' | 'category'>('daily');

  const { data: dailyReport, isLoading: isDailyLoading } = useQuery({
    queryKey: ['dailyReport'],
    queryFn: getDailyReport,
    enabled: viewMode === 'daily',
  });

  const { data: categoryReport, isLoading: isCategoryLoading } = useQuery({
    queryKey: ['categoryReport'],
    queryFn: getCategoryReport,
    enabled: viewMode === 'category',
  });

  const isLoading = (viewMode === 'daily' && isDailyLoading) || (viewMode === 'category' && isCategoryLoading);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
            <FileText className="w-7 h-7 text-red-600" />
            AI 뉴스 리포트
          </h1>
          <p className="text-gray-500 mt-1">HIGH 중요도 기사 요약 분석</p>
        </div>
        <div className="flex gap-2">
          <button
            onClick={() => setViewMode('daily')}
            className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
              viewMode === 'daily'
                ? 'bg-red-600 text-white'
                : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
            }`}
          >
            일일 리포트
          </button>
          <button
            onClick={() => setViewMode('category')}
            className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
              viewMode === 'category'
                ? 'bg-red-600 text-white'
                : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
            }`}
          >
            카테고리별
          </button>
        </div>
      </div>

      {isLoading ? (
        <div className="flex items-center justify-center h-64">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-red-600" />
        </div>
      ) : viewMode === 'daily' && dailyReport ? (
        <div className="space-y-6">
          {/* Executive Summary */}
          <div className="bg-gradient-to-r from-red-600 to-orange-500 rounded-xl p-6 text-white">
            <div className="flex items-start gap-4">
              <AlertCircle className="w-8 h-8 flex-shrink-0 mt-1" />
              <div>
                <h2 className="text-lg font-semibold mb-2">Executive Summary</h2>
                <p className="text-red-100 leading-relaxed">{dailyReport.executiveSummary}</p>
              </div>
            </div>
          </div>

          {/* Stats Cards */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="bg-white rounded-xl p-5 shadow-sm border border-gray-200">
              <div className="text-sm text-gray-500 mb-1">중요 기사 수</div>
              <div className="text-3xl font-bold text-red-600">
                {dailyReport.totalHighImportanceArticles}
              </div>
            </div>
            <div className="bg-white rounded-xl p-5 shadow-sm border border-gray-200">
              <div className="text-sm text-gray-500 mb-1">분석된 카테고리</div>
              <div className="text-3xl font-bold text-blue-600">
                {Object.keys(dailyReport.categoryDistribution).length}
              </div>
            </div>
            <div className="bg-white rounded-xl p-5 shadow-sm border border-gray-200">
              <div className="text-sm text-gray-500 mb-1">리포트 생성일</div>
              <div className="text-xl font-bold text-gray-700">
                {dailyReport.period}
              </div>
            </div>
          </div>

          {/* Category Distribution */}
          <div className="bg-white rounded-xl p-5 shadow-sm border border-gray-200">
            <h3 className="font-semibold text-gray-900 mb-4">카테고리별 분포</h3>
            <div className="flex flex-wrap gap-2">
              {Object.entries(dailyReport.categoryDistribution)
                .sort((a, b) => b[1] - a[1])
                .map(([category, count]) => (
                  <span
                    key={category}
                    className={`px-3 py-1.5 rounded-full text-sm font-medium ${CATEGORY_COLORS[category] || CATEGORY_COLORS.OTHER}`}
                  >
                    {category}: {count}건
                  </span>
                ))}
            </div>
          </div>

          {/* Articles List */}
          <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
            <div className="p-4 bg-gray-50 border-b">
              <h3 className="font-semibold text-gray-900">중요 기사 목록</h3>
            </div>
            <div className="p-4 space-y-3">
              {dailyReport.articles.length === 0 ? (
                <div className="text-center text-gray-500 py-8">
                  분석된 중요 기사가 없습니다.
                </div>
              ) : (
                dailyReport.articles.map((article) => (
                  <ArticleCard key={article.id} article={article} />
                ))
              )}
            </div>
          </div>
        </div>
      ) : viewMode === 'category' && categoryReport ? (
        <div className="space-y-4">
          {categoryReport.length === 0 ? (
            <div className="text-center text-gray-500 py-16 bg-white rounded-xl">
              분석된 중요 기사가 없습니다.
            </div>
          ) : (
            categoryReport.map((category) => (
              <CategorySection key={category.category} category={category} />
            ))
          )}
        </div>
      ) : null}
    </div>
  );
}
