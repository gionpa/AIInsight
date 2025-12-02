import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  getDailyReport,
  getCategoryReport,
  getAllTopicsReport,
  getInterestTopics,
  createInterestTopic,
  updateInterestTopic,
  deleteInterestTopic,
  initializeDefaultTopics,
} from '../api';
import {
  FileText,
  AlertCircle,
  TrendingUp,
  ExternalLink,
  Clock,
  Tag,
  ChevronDown,
  ChevronUp,
  Plus,
  Edit2,
  Trash2,
  Star,
  Settings,
  X,
  Save,
  RefreshCw,
} from 'lucide-react';
import type { CategoryReport, ArticleSummary, InterestTopic, TopicReportResponse, NewsArticle } from '../types';

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

const IMPORTANCE_COLORS: Record<string, string> = {
  HIGH: 'bg-red-100 text-red-800',
  MEDIUM: 'bg-yellow-100 text-yellow-800',
  LOW: 'bg-gray-100 text-gray-800',
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

function TopicArticleCard({ article }: { article: NewsArticle }) {
  const [expanded, setExpanded] = useState(false);

  return (
    <div className="bg-white rounded-lg border border-gray-200 p-4 hover:shadow-md transition-shadow">
      <div className="flex items-start justify-between gap-3">
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 mb-2">
            {article.importance && (
              <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${IMPORTANCE_COLORS[article.importance] || IMPORTANCE_COLORS.LOW}`}>
                {article.importance}
              </span>
            )}
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
          <div className="flex items-center gap-3 text-xs text-gray-500 mb-2">
            <span className="flex items-center gap-1">
              <Tag className="w-3 h-3" />
              {article.targetName}
            </span>
            <span className="flex items-center gap-1">
              <Clock className="w-3 h-3" />
              {new Date(article.crawledAt).toLocaleString('ko-KR', {
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

function TopicSection({ topic }: { topic: TopicReportResponse }) {
  const [collapsed, setCollapsed] = useState(false);

  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
      <button
        onClick={() => setCollapsed(!collapsed)}
        className="w-full flex items-center justify-between p-4 bg-gradient-to-r from-blue-50 to-indigo-50 hover:from-blue-100 hover:to-indigo-100 transition-colors"
      >
        <div className="flex items-center gap-3">
          <Star className="w-5 h-5 text-yellow-500" />
          <span className="font-semibold text-gray-900">{topic.topicName}</span>
          <span className="text-sm text-gray-500">
            {topic.totalArticles}개 기사
          </span>
          {topic.highImportanceCount > 0 && (
            <span className="px-2 py-0.5 rounded-full text-xs font-medium bg-red-100 text-red-800">
              HIGH {topic.highImportanceCount}
            </span>
          )}
        </div>
        {collapsed ? <ChevronDown className="w-5 h-5 text-gray-400" /> : <ChevronUp className="w-5 h-5 text-gray-400" />}
      </button>
      {!collapsed && (
        <div className="p-4">
          {topic.description && (
            <p className="text-sm text-gray-500 mb-3">{topic.description}</p>
          )}
          <div className="text-xs text-gray-400 mb-3">
            키워드: {topic.keywords}
          </div>
          <div className="space-y-3">
            {topic.articles.length === 0 ? (
              <div className="text-center text-gray-500 py-4">
                해당 주제의 기사가 없습니다.
              </div>
            ) : (
              topic.articles.map((article) => (
                <TopicArticleCard key={article.id} article={article} />
              ))
            )}
          </div>
        </div>
      )}
    </div>
  );
}

interface TopicFormData {
  name: string;
  description: string;
  keywords: string;
}

function TopicManageModal({
  isOpen,
  onClose,
  topics,
  onRefresh,
}: {
  isOpen: boolean;
  onClose: () => void;
  topics: InterestTopic[];
  onRefresh: () => void;
}) {
  const queryClient = useQueryClient();
  const [editingId, setEditingId] = useState<number | null>(null);
  const [isAdding, setIsAdding] = useState(false);
  const [formData, setFormData] = useState<TopicFormData>({ name: '', description: '', keywords: '' });

  const createMutation = useMutation({
    mutationFn: createInterestTopic,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['interestTopics'] });
      queryClient.invalidateQueries({ queryKey: ['allTopicsReport'] });
      setIsAdding(false);
      setFormData({ name: '', description: '', keywords: '' });
      onRefresh();
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: Partial<TopicFormData> }) =>
      updateInterestTopic(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['interestTopics'] });
      queryClient.invalidateQueries({ queryKey: ['allTopicsReport'] });
      setEditingId(null);
      onRefresh();
    },
  });

  const deleteMutation = useMutation({
    mutationFn: deleteInterestTopic,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['interestTopics'] });
      queryClient.invalidateQueries({ queryKey: ['allTopicsReport'] });
      onRefresh();
    },
  });

  const initMutation = useMutation({
    mutationFn: initializeDefaultTopics,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['interestTopics'] });
      queryClient.invalidateQueries({ queryKey: ['allTopicsReport'] });
      onRefresh();
    },
  });

  const handleEdit = (topic: InterestTopic) => {
    setEditingId(topic.id);
    setFormData({
      name: topic.name,
      description: topic.description || '',
      keywords: topic.keywords,
    });
  };

  const handleSave = () => {
    if (editingId) {
      updateMutation.mutate({ id: editingId, data: formData });
    }
  };

  const handleCreate = () => {
    if (formData.name && formData.keywords) {
      createMutation.mutate(formData);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-xl shadow-xl max-w-2xl w-full max-h-[80vh] overflow-hidden">
        <div className="flex items-center justify-between p-4 border-b">
          <h2 className="text-lg font-semibold flex items-center gap-2">
            <Settings className="w-5 h-5" />
            관심 주제 관리
          </h2>
          <button onClick={onClose} className="p-2 hover:bg-gray-100 rounded-lg">
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-4 overflow-y-auto max-h-[60vh]">
          {topics.length === 0 && !isAdding && (
            <div className="text-center py-8">
              <p className="text-gray-500 mb-4">등록된 관심 주제가 없습니다.</p>
              <button
                onClick={() => initMutation.mutate()}
                disabled={initMutation.isPending}
                className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50"
              >
                {initMutation.isPending ? '초기화 중...' : '기본 주제 6개 초기화'}
              </button>
            </div>
          )}

          <div className="space-y-3">
            {topics.map((topic) => (
              <div key={topic.id} className="border rounded-lg p-3">
                {editingId === topic.id ? (
                  <div className="space-y-3">
                    <input
                      type="text"
                      value={formData.name}
                      onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                      placeholder="주제 이름"
                      className="w-full px-3 py-2 border rounded-lg"
                    />
                    <input
                      type="text"
                      value={formData.description}
                      onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                      placeholder="설명 (선택)"
                      className="w-full px-3 py-2 border rounded-lg"
                    />
                    <input
                      type="text"
                      value={formData.keywords}
                      onChange={(e) => setFormData({ ...formData, keywords: e.target.value })}
                      placeholder="키워드 (쉼표 구분)"
                      className="w-full px-3 py-2 border rounded-lg"
                    />
                    <div className="flex gap-2">
                      <button
                        onClick={handleSave}
                        disabled={updateMutation.isPending}
                        className="px-3 py-1.5 bg-blue-600 text-white rounded-lg text-sm hover:bg-blue-700 disabled:opacity-50"
                      >
                        <Save className="w-4 h-4 inline mr-1" />
                        저장
                      </button>
                      <button
                        onClick={() => setEditingId(null)}
                        className="px-3 py-1.5 bg-gray-200 text-gray-700 rounded-lg text-sm hover:bg-gray-300"
                      >
                        취소
                      </button>
                    </div>
                  </div>
                ) : (
                  <div className="flex items-center justify-between">
                    <div>
                      <div className="font-medium">{topic.name}</div>
                      <div className="text-sm text-gray-500">{topic.keywords}</div>
                      {topic.description && (
                        <div className="text-xs text-gray-400 mt-1">{topic.description}</div>
                      )}
                    </div>
                    <div className="flex gap-1">
                      <button
                        onClick={() => handleEdit(topic)}
                        className="p-2 text-gray-500 hover:text-blue-600 hover:bg-blue-50 rounded-lg"
                      >
                        <Edit2 className="w-4 h-4" />
                      </button>
                      <button
                        onClick={() => {
                          if (confirm('이 주제를 삭제하시겠습니까?')) {
                            deleteMutation.mutate(topic.id);
                          }
                        }}
                        disabled={deleteMutation.isPending}
                        className="p-2 text-gray-500 hover:text-red-600 hover:bg-red-50 rounded-lg"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  </div>
                )}
              </div>
            ))}

            {isAdding && (
              <div className="border rounded-lg p-3 bg-blue-50 space-y-3">
                <input
                  type="text"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  placeholder="주제 이름"
                  className="w-full px-3 py-2 border rounded-lg"
                />
                <input
                  type="text"
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  placeholder="설명 (선택)"
                  className="w-full px-3 py-2 border rounded-lg"
                />
                <input
                  type="text"
                  value={formData.keywords}
                  onChange={(e) => setFormData({ ...formData, keywords: e.target.value })}
                  placeholder="키워드 (쉼표 구분, 예: google, gemini, 구글)"
                  className="w-full px-3 py-2 border rounded-lg"
                />
                <div className="flex gap-2">
                  <button
                    onClick={handleCreate}
                    disabled={createMutation.isPending || !formData.name || !formData.keywords}
                    className="px-3 py-1.5 bg-blue-600 text-white rounded-lg text-sm hover:bg-blue-700 disabled:opacity-50"
                  >
                    <Plus className="w-4 h-4 inline mr-1" />
                    추가
                  </button>
                  <button
                    onClick={() => {
                      setIsAdding(false);
                      setFormData({ name: '', description: '', keywords: '' });
                    }}
                    className="px-3 py-1.5 bg-gray-200 text-gray-700 rounded-lg text-sm hover:bg-gray-300"
                  >
                    취소
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>

        <div className="flex justify-between p-4 border-t bg-gray-50">
          {!isAdding && (
            <button
              onClick={() => {
                setIsAdding(true);
                setFormData({ name: '', description: '', keywords: '' });
              }}
              className="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 flex items-center gap-2"
            >
              <Plus className="w-4 h-4" />
              새 주제 추가
            </button>
          )}
          <button
            onClick={onClose}
            className="px-4 py-2 bg-gray-600 text-white rounded-lg hover:bg-gray-700 ml-auto"
          >
            닫기
          </button>
        </div>
      </div>
    </div>
  );
}

export default function Report() {
  const [viewMode, setViewMode] = useState<'daily' | 'category' | 'topics'>('daily');
  const [showTopicManager, setShowTopicManager] = useState(false);

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

  const { data: topicsReport, isLoading: isTopicsLoading, refetch: refetchTopicsReport } = useQuery({
    queryKey: ['allTopicsReport'],
    queryFn: () => getAllTopicsReport(5),
    enabled: viewMode === 'topics',
  });

  const { data: interestTopics = [], refetch: refetchTopics } = useQuery({
    queryKey: ['interestTopics'],
    queryFn: getInterestTopics,
  });

  const isLoading =
    (viewMode === 'daily' && isDailyLoading) ||
    (viewMode === 'category' && isCategoryLoading) ||
    (viewMode === 'topics' && isTopicsLoading);

  const handleRefresh = () => {
    refetchTopics();
    refetchTopicsReport();
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between gap-4 min-w-0">
        <div className="flex items-center gap-2 min-w-0 flex-shrink-0">
          <FileText className="w-6 h-6 md:w-7 md:h-7 text-red-600 flex-shrink-0" />
          <h1 className="text-lg md:text-2xl font-bold text-gray-900 whitespace-nowrap">리포트</h1>
        </div>
        <div className="flex gap-1 md:gap-2 flex-shrink-0 overflow-x-auto">
          <button
            onClick={() => setViewMode('daily')}
            className={`px-2 md:px-4 py-1.5 md:py-2 rounded-lg text-xs md:text-sm font-medium transition-colors whitespace-nowrap ${
              viewMode === 'daily'
                ? 'bg-red-600 text-white'
                : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
            }`}
          >
            일일
          </button>
          <button
            onClick={() => setViewMode('category')}
            className={`px-2 md:px-4 py-1.5 md:py-2 rounded-lg text-xs md:text-sm font-medium transition-colors whitespace-nowrap ${
              viewMode === 'category'
                ? 'bg-red-600 text-white'
                : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
            }`}
          >
            카테고리
          </button>
          <button
            onClick={() => setViewMode('topics')}
            className={`px-2 md:px-4 py-1.5 md:py-2 rounded-lg text-xs md:text-sm font-medium transition-colors whitespace-nowrap ${
              viewMode === 'topics'
                ? 'bg-blue-600 text-white'
                : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
            }`}
          >
            <Star className="w-3 h-3 md:w-4 md:h-4 inline mr-0.5 md:mr-1" />
            관심주제
          </button>
        </div>
      </div>

      {/* Topics Management Bar */}
      {viewMode === 'topics' && (
        <div className="bg-gradient-to-r from-blue-50 to-indigo-50 rounded-xl p-4 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <Star className="w-6 h-6 text-yellow-500" />
            <div>
              <h2 className="font-semibold text-gray-900">관심 주제 리포트</h2>
              <p className="text-sm text-gray-500">
                {interestTopics.length}개 주제 등록됨
              </p>
            </div>
          </div>
          <div className="flex gap-2">
            <button
              onClick={() => {
                refetchTopicsReport();
              }}
              className="px-3 py-2 bg-white text-gray-700 rounded-lg hover:bg-gray-50 border flex items-center gap-1"
            >
              <RefreshCw className="w-4 h-4" />
              새로고침
            </button>
            <button
              onClick={() => setShowTopicManager(true)}
              className="px-3 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 flex items-center gap-1"
            >
              <Settings className="w-4 h-4" />
              주제 관리
            </button>
          </div>
        </div>
      )}

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
      ) : viewMode === 'topics' && topicsReport ? (
        <div className="space-y-4">
          {/* Summary Stats */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="bg-white rounded-xl p-5 shadow-sm border border-gray-200">
              <div className="text-sm text-gray-500 mb-1">등록 주제</div>
              <div className="text-3xl font-bold text-blue-600">
                {topicsReport.totalTopics}
              </div>
            </div>
            <div className="bg-white rounded-xl p-5 shadow-sm border border-gray-200">
              <div className="text-sm text-gray-500 mb-1">관련 기사</div>
              <div className="text-3xl font-bold text-green-600">
                {topicsReport.totalArticles}
              </div>
            </div>
            <div className="bg-white rounded-xl p-5 shadow-sm border border-gray-200">
              <div className="text-sm text-gray-500 mb-1">생성 시간</div>
              <div className="text-lg font-bold text-gray-700">
                {new Date(topicsReport.generatedAt).toLocaleString('ko-KR', {
                  hour: '2-digit',
                  minute: '2-digit',
                })}
              </div>
            </div>
          </div>

          {topicsReport.topics.length === 0 ? (
            <div className="text-center text-gray-500 py-16 bg-white rounded-xl">
              <Star className="w-12 h-12 mx-auto mb-4 text-gray-300" />
              <p className="mb-4">등록된 관심 주제가 없습니다.</p>
              <button
                onClick={() => setShowTopicManager(true)}
                className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
              >
                주제 추가하기
              </button>
            </div>
          ) : (
            topicsReport.topics.map((topic) => (
              <TopicSection key={topic.topicId} topic={topic} />
            ))
          )}
        </div>
      ) : null}

      {/* Topic Manager Modal */}
      <TopicManageModal
        isOpen={showTopicManager}
        onClose={() => setShowTopicManager(false)}
        topics={interestTopics}
        onRefresh={handleRefresh}
      />
    </div>
  );
}
