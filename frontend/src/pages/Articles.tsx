import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getArticles, searchArticles, markArticleAsRead, getArticle, getArticlesByImportance } from '../api';
import type { NewsArticle, ArticleCategory, ArticleImportance } from '../types';
import {
  Search,
  ExternalLink,
  Eye,
  X,
  Star,
  Tag,
  Filter,
} from 'lucide-react';

const categoryLabels: Record<ArticleCategory, string> = {
  LLM: 'LLM',
  COMPUTER_VISION: '컴퓨터 비전',
  NLP: '자연어 처리',
  ROBOTICS: '로보틱스',
  ML_OPS: 'MLOps',
  RESEARCH: '연구',
  INDUSTRY: '산업',
  STARTUP: '스타트업',
  REGULATION: '규제/정책',
  TUTORIAL: '튜토리얼',
  PRODUCT: '제품',
  OTHER: '기타',
};

const importanceColors = {
  HIGH: 'bg-red-100 text-red-700',
  MEDIUM: 'bg-yellow-100 text-yellow-700',
  LOW: 'bg-gray-100 text-gray-700',
};

export default function Articles() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [searchKeyword, setSearchKeyword] = useState('');
  const [isSearching, setIsSearching] = useState(false);
  const [selectedArticle, setSelectedArticle] = useState<number | null>(null);
  const [importanceFilter, setImportanceFilter] = useState<ArticleImportance | 'ALL'>('ALL');

  const { data, isLoading } = useQuery({
    queryKey: ['articles', page, isSearching ? searchKeyword : '', importanceFilter],
    queryFn: () => {
      if (isSearching && searchKeyword) {
        return searchArticles(searchKeyword, page, 20);
      }
      if (importanceFilter !== 'ALL') {
        return getArticlesByImportance(importanceFilter, page, 20);
      }
      return getArticles(page, 20);
    },
  });

  const { data: articleDetail, isLoading: isLoadingDetail } = useQuery({
    queryKey: ['article', selectedArticle],
    queryFn: () => getArticle(selectedArticle!),
    enabled: selectedArticle !== null,
  });

  const markReadMutation = useMutation({
    mutationFn: markArticleAsRead,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['articles'] });
    },
  });

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setIsSearching(true);
    setPage(0);
  };

  const clearSearch = () => {
    setSearchKeyword('');
    setIsSearching(false);
    setImportanceFilter('ALL');
    setPage(0);
  };

  const handleImportanceFilter = (importance: ArticleImportance | 'ALL') => {
    setImportanceFilter(importance);
    setIsSearching(false);
    setSearchKeyword('');
    setPage(0);
  };

  const handleViewArticle = (article: NewsArticle) => {
    setSelectedArticle(article.id);
    if (article.isNew) {
      markReadMutation.mutate(article.id);
    }
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  return (
    <div>
      <div className="flex justify-between items-center gap-2 mb-4 min-w-0">
        <div className="flex items-center gap-2 flex-shrink-0">
          <h1 className="text-lg md:text-2xl font-bold whitespace-nowrap">뉴스 기사</h1>
          <span className="text-xs md:text-sm text-gray-500 whitespace-nowrap">
            ({data?.totalElements?.toLocaleString() || 0}개)
          </span>
        </div>

        {/* Search */}
        <form onSubmit={handleSearch} className="flex gap-1 md:gap-2 flex-shrink-0">
          <div className="relative">
            <input
              type="text"
              value={searchKeyword}
              onChange={(e) => setSearchKeyword(e.target.value)}
              placeholder="검색..."
              className="pl-8 md:pl-10 pr-2 md:pr-4 py-1.5 md:py-2 border rounded-lg w-24 md:w-64 text-sm md:text-base"
            />
            <Search className="absolute left-2 md:left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          </div>
          {(isSearching || importanceFilter !== 'ALL') && (
            <button
              type="button"
              onClick={clearSearch}
              className="px-2 md:px-3 py-1.5 md:py-2 text-xs md:text-sm text-gray-600 hover:bg-gray-100 rounded-lg whitespace-nowrap"
            >
              초기화
            </button>
          )}
          <button
            type="submit"
            className="px-2 md:px-4 py-1.5 md:py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 text-xs md:text-sm whitespace-nowrap"
          >
            검색
          </button>
        </form>
      </div>

      {/* Importance Filter */}
      <div className="flex items-center gap-1 md:gap-2 mb-6 flex-wrap md:flex-nowrap">
        <Filter className="w-3 h-3 md:w-4 md:h-4 text-gray-500 flex-shrink-0" />
        <span className="text-xs md:text-sm text-gray-600 mr-1 md:mr-2 whitespace-nowrap">중요도:</span>
        <div className="flex gap-1">
          {(['ALL', 'HIGH', 'MEDIUM', 'LOW'] as const).map((level) => (
            <button
              key={level}
              onClick={() => handleImportanceFilter(level)}
              className={`px-2 md:px-3 py-1 md:py-1.5 text-xs md:text-sm rounded-lg transition-colors whitespace-nowrap ${
                importanceFilter === level
                  ? level === 'ALL'
                    ? 'bg-blue-600 text-white'
                    : level === 'HIGH'
                    ? 'bg-red-600 text-white'
                    : level === 'MEDIUM'
                    ? 'bg-yellow-500 text-white'
                    : 'bg-gray-600 text-white'
                  : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
              }`}
            >
              {level === 'ALL' ? '전체' : level === 'HIGH' ? '높음' : level === 'MEDIUM' ? '중간' : '낮음'}
            </button>
          ))}
        </div>
        {importanceFilter !== 'ALL' && (
          <span className="ml-1 md:ml-2 text-xs md:text-sm text-gray-500 whitespace-nowrap">
            ({data?.totalElements || 0}개)
          </span>
        )}
      </div>

      {/* Articles Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {data?.content.map((article) => (
          <div
            key={article.id}
            className={`bg-white rounded-lg shadow overflow-hidden hover:shadow-md transition-shadow ${
              article.isNew ? 'ring-2 ring-blue-500' : ''
            }`}
          >
            {article.thumbnailUrl && (
              <img
                src={article.thumbnailUrl}
                alt={article.title}
                className="w-full h-40 object-cover"
              />
            )}
            <div className="p-4">
              <div className="flex items-start justify-between mb-1">
                <h3 className="font-medium line-clamp-2 flex-1">{article.titleKo || article.title}</h3>
                {article.isNew && (
                  <span className="ml-2 px-2 py-0.5 bg-blue-500 text-white text-xs rounded">
                    NEW
                  </span>
                )}
              </div>
              <div className="text-xs text-gray-400 mb-2">ID: {article.id}</div>

              {article.summary && (
                <p className="text-sm text-gray-600 line-clamp-2 mb-3">
                  {article.summary}
                </p>
              )}

              <div className="flex flex-wrap gap-2 mb-3">
                {article.category && (
                  <span className="flex items-center gap-1 px-2 py-0.5 bg-purple-100 text-purple-700 text-xs rounded">
                    <Tag className="w-3 h-3" />
                    {categoryLabels[article.category]}
                  </span>
                )}
                {article.importance && (
                  <span
                    className={`px-2 py-0.5 text-xs rounded ${importanceColors[article.importance]}`}
                  >
                    {article.importance}
                  </span>
                )}
                {article.relevanceScore !== undefined && (
                  <span className="flex items-center gap-1 px-2 py-0.5 bg-green-100 text-green-700 text-xs rounded">
                    <Star className="w-3 h-3" />
                    {(article.relevanceScore * 100).toFixed(0)}%
                  </span>
                )}
              </div>

              <div className="flex justify-between items-center text-xs text-gray-500">
                <span>{article.targetName}</span>
                <span>
                  {new Date(article.crawledAt).toLocaleString('ko-KR', {
                    year: 'numeric',
                    month: '2-digit',
                    day: '2-digit',
                    hour: '2-digit',
                    minute: '2-digit'
                  })}
                </span>
              </div>

              <div className="flex gap-2 mt-3">
                <button
                  onClick={() => handleViewArticle(article)}
                  className="flex-1 flex items-center justify-center gap-1 px-3 py-1.5 bg-gray-100 text-gray-700 rounded hover:bg-gray-200"
                >
                  <Eye className="w-4 h-4" />
                  상세보기
                </button>
                <a
                  href={article.originalUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="flex items-center justify-center gap-1 px-3 py-1.5 bg-blue-100 text-blue-700 rounded hover:bg-blue-200"
                >
                  <ExternalLink className="w-4 h-4" />
                  원문
                </a>
              </div>
            </div>
          </div>
        ))}
      </div>

      {data?.content.length === 0 && (
        <div className="text-center py-12 text-gray-500">
          {isSearching ? '검색 결과가 없습니다.' : '기사가 없습니다.'}
        </div>
      )}

      {/* Pagination */}
      {data && data.totalPages > 1 && (
        <div className="flex justify-center gap-2 mt-6">
          <button
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            disabled={data.first}
            className="px-4 py-2 bg-white border rounded-lg disabled:opacity-50"
          >
            이전
          </button>
          <span className="px-4 py-2 text-gray-600">
            {data.number + 1} / {data.totalPages}
          </span>
          <button
            onClick={() => setPage((p) => p + 1)}
            disabled={data.last}
            className="px-4 py-2 bg-white border rounded-lg disabled:opacity-50"
          >
            다음
          </button>
        </div>
      )}

      {/* Article Detail Modal */}
      {selectedArticle !== null && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-lg shadow-xl w-full max-w-3xl max-h-[90vh] overflow-y-auto">
            <div className="sticky top-0 bg-white flex justify-between items-center p-4 border-b">
              <h2 className="text-lg font-semibold">기사 상세</h2>
              <button
                onClick={() => setSelectedArticle(null)}
                className="p-1 hover:bg-gray-100 rounded"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {isLoadingDetail ? (
              <div className="flex items-center justify-center h-64">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
              </div>
            ) : articleDetail ? (
              <div className="p-6">
                <h1 className="text-2xl font-bold mb-1">{articleDetail.titleKo || articleDetail.title}</h1>
                <div className="text-xs text-gray-400 mb-4">ID: {articleDetail.id}</div>

                <div className="flex flex-wrap gap-2 mb-4">
                  {articleDetail.category && (
                    <span className="px-3 py-1 bg-purple-100 text-purple-700 rounded-full text-sm">
                      {categoryLabels[articleDetail.category]}
                    </span>
                  )}
                  {articleDetail.importance && (
                    <span
                      className={`px-3 py-1 rounded-full text-sm ${importanceColors[articleDetail.importance]}`}
                    >
                      중요도: {articleDetail.importance}
                    </span>
                  )}
                  {articleDetail.relevanceScore !== undefined && (
                    <span className="px-3 py-1 bg-green-100 text-green-700 rounded-full text-sm">
                      관련성: {(articleDetail.relevanceScore * 100).toFixed(0)}%
                    </span>
                  )}
                </div>

                <div className="flex gap-4 text-sm text-gray-500 mb-6">
                  <span>출처: {articleDetail.targetName}</span>
                  {articleDetail.author && <span>작성자: {articleDetail.author}</span>}
                  {articleDetail.publishedAt && (
                    <span>게시일: {new Date(articleDetail.publishedAt).toLocaleString('ko-KR', {
                      year: 'numeric',
                      month: '2-digit',
                      day: '2-digit',
                      hour: '2-digit',
                      minute: '2-digit'
                    })}</span>
                  )}
                </div>

                {articleDetail.summary && (
                  <div className="bg-blue-50 p-4 rounded-lg mb-6">
                    <h3 className="font-semibold text-blue-800 mb-2">AI 요약</h3>
                    <p className="text-blue-900">{articleDetail.summary}</p>
                  </div>
                )}

                {articleDetail.content && (
                  <div className="prose max-w-none">
                    <h3 className="font-semibold mb-2">본문</h3>
                    <p className="whitespace-pre-wrap text-gray-700">{articleDetail.content}</p>
                  </div>
                )}

                <div className="mt-6 pt-4 border-t">
                  <a
                    href={articleDetail.originalUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="inline-flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
                  >
                    <ExternalLink className="w-4 h-4" />
                    원문 보기
                  </a>
                </div>
              </div>
            ) : null}
          </div>
        </div>
      )}
    </div>
  );
}
