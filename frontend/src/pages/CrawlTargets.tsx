import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  getCrawlTargets,
  createCrawlTarget,
  updateCrawlTarget,
  deleteCrawlTarget,
  toggleCrawlTarget,
  executeCrawl,
} from '../api';
import type { CrawlTarget, CreateCrawlTargetRequest } from '../types';
import {
  Plus,
  Edit,
  Trash2,
  Play,
  Power,
  ExternalLink,
  X,
  Loader2,
} from 'lucide-react';

export default function CrawlTargets() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingTarget, setEditingTarget] = useState<CrawlTarget | null>(null);
  const [executingTargetId, setExecutingTargetId] = useState<number | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ['crawlTargets', page],
    queryFn: () => getCrawlTargets(page, 20),
  });

  const createMutation = useMutation({
    mutationFn: createCrawlTarget,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['crawlTargets'] });
      setIsModalOpen(false);
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: CreateCrawlTargetRequest }) =>
      updateCrawlTarget(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['crawlTargets'] });
      setEditingTarget(null);
      setIsModalOpen(false);
    },
  });

  const deleteMutation = useMutation({
    mutationFn: deleteCrawlTarget,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['crawlTargets'] });
    },
  });

  const toggleMutation = useMutation({
    mutationFn: toggleCrawlTarget,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['crawlTargets'] });
    },
  });

  const executeMutation = useMutation({
    mutationFn: executeCrawl,
    onSuccess: (result) => {
      setExecutingTargetId(null);
      alert(
        result.success
          ? `크롤링 완료: ${result.articlesFound}개 기사 발견`
          : `크롤링 실패: ${result.errorMessage}`
      );
      queryClient.invalidateQueries({ queryKey: ['crawlTargets'] });
    },
    onError: () => {
      setExecutingTargetId(null);
    },
  });

  const handleExecuteCrawl = (targetId: number) => {
    setExecutingTargetId(targetId);
    executeMutation.mutate(targetId);
  };

  const handleSubmit = (formData: CreateCrawlTargetRequest) => {
    if (editingTarget) {
      updateMutation.mutate({ id: editingTarget.id, data: formData });
    } else {
      createMutation.mutate(formData);
    }
  };

  const handleDelete = (target: CrawlTarget) => {
    if (confirm(`"${target.name}"을(를) 삭제하시겠습니까?`)) {
      deleteMutation.mutate(target.id);
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
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">크롤링 타겟</h1>
        <button
          onClick={() => {
            setEditingTarget(null);
            setIsModalOpen(true);
          }}
          className="flex items-center gap-2 bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700"
        >
          <Plus className="w-5 h-5" />
          타겟 추가
        </button>
      </div>

      {/* Table */}
      <div className="bg-white rounded-lg shadow overflow-hidden">
        <table className="w-full">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-4 py-3 text-left text-sm font-medium text-gray-500">이름</th>
              <th className="px-4 py-3 text-left text-sm font-medium text-gray-500">URL</th>
              <th className="px-4 py-3 text-left text-sm font-medium text-gray-500">주기</th>
              <th className="px-4 py-3 text-left text-sm font-medium text-gray-500">상태</th>
              <th className="px-4 py-3 text-left text-sm font-medium text-gray-500">마지막 크롤링</th>
              <th className="px-4 py-3 text-left text-sm font-medium text-gray-500">동작</th>
            </tr>
          </thead>
          <tbody className="divide-y">
            {data?.content.map((target) => (
              <tr key={target.id} className="hover:bg-gray-50">
                <td className="px-4 py-3 font-medium">{target.name}</td>
                <td className="px-4 py-3">
                  <a
                    href={target.url}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-blue-600 hover:underline flex items-center gap-1"
                  >
                    {new URL(target.url).hostname}
                    <ExternalLink className="w-3 h-3" />
                  </a>
                </td>
                <td className="px-4 py-3 text-sm text-gray-500">{target.cronExpression}</td>
                <td className="px-4 py-3">
                  <span
                    className={`px-2 py-1 rounded text-xs font-medium ${
                      target.enabled
                        ? 'bg-green-100 text-green-700'
                        : 'bg-gray-100 text-gray-700'
                    }`}
                  >
                    {target.enabled ? '활성' : '비활성'}
                  </span>
                  {target.lastStatus && (
                    <span
                      className={`ml-2 px-2 py-1 rounded text-xs font-medium ${
                        target.lastStatus === 'SUCCESS'
                          ? 'bg-blue-100 text-blue-700'
                          : target.lastStatus === 'FAILED'
                          ? 'bg-red-100 text-red-700'
                          : 'bg-yellow-100 text-yellow-700'
                      }`}
                    >
                      {target.lastStatus}
                    </span>
                  )}
                </td>
                <td className="px-4 py-3 text-sm text-gray-500">
                  {target.lastCrawledAt
                    ? new Date(target.lastCrawledAt).toLocaleString('ko-KR', {
                        timeZone: 'Asia/Seoul',
                        year: 'numeric',
                        month: '2-digit',
                        day: '2-digit',
                        hour: '2-digit',
                        minute: '2-digit',
                      })
                    : '-'}
                </td>
                <td className="px-4 py-3">
                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => handleExecuteCrawl(target.id)}
                      disabled={executingTargetId !== null}
                      className={`p-1 rounded cursor-pointer ${
                        executingTargetId === target.id
                          ? 'text-blue-600 bg-blue-50'
                          : 'text-green-600 hover:bg-green-50'
                      } disabled:opacity-50 disabled:cursor-not-allowed`}
                      title={executingTargetId === target.id ? '크롤링 중...' : '크롤링 실행'}
                    >
                      {executingTargetId === target.id ? (
                        <Loader2 className="w-5 h-5 animate-spin" />
                      ) : (
                        <Play className="w-5 h-5" />
                      )}
                    </button>
                    <button
                      onClick={() => toggleMutation.mutate(target.id)}
                      className="p-1 text-yellow-600 hover:bg-yellow-50 rounded cursor-pointer"
                      title={target.enabled ? '비활성화' : '활성화'}
                    >
                      <Power className="w-5 h-5" />
                    </button>
                    <button
                      onClick={() => {
                        setEditingTarget(target);
                        setIsModalOpen(true);
                      }}
                      className="p-1 text-blue-600 hover:bg-blue-50 rounded cursor-pointer"
                      title="수정"
                    >
                      <Edit className="w-5 h-5" />
                    </button>
                    <button
                      onClick={() => handleDelete(target)}
                      className="p-1 text-red-600 hover:bg-red-50 rounded cursor-pointer"
                      title="삭제"
                    >
                      <Trash2 className="w-5 h-5" />
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        {/* Pagination */}
        {data && data.totalPages > 1 && (
          <div className="px-4 py-3 bg-gray-50 flex justify-between items-center">
            <button
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={data.first}
              className="px-3 py-1 bg-white border rounded disabled:opacity-50"
            >
              이전
            </button>
            <span className="text-sm text-gray-500">
              {data.number + 1} / {data.totalPages} 페이지
            </span>
            <button
              onClick={() => setPage((p) => p + 1)}
              disabled={data.last}
              className="px-3 py-1 bg-white border rounded disabled:opacity-50"
            >
              다음
            </button>
          </div>
        )}
      </div>

      {/* Modal */}
      {isModalOpen && (
        <TargetFormModal
          target={editingTarget}
          onSubmit={handleSubmit}
          onClose={() => {
            setIsModalOpen(false);
            setEditingTarget(null);
          }}
          isLoading={createMutation.isPending || updateMutation.isPending}
        />
      )}
    </div>
  );
}

function TargetFormModal({
  target,
  onSubmit,
  onClose,
  isLoading,
}: {
  target: CrawlTarget | null;
  onSubmit: (data: CreateCrawlTargetRequest) => void;
  onClose: () => void;
  isLoading: boolean;
}) {
  const [formData, setFormData] = useState<CreateCrawlTargetRequest>({
    name: target?.name || '',
    url: target?.url || '',
    description: target?.description || '',
    cronExpression: target?.cronExpression || '0 0 * * * *',
    selectorConfig: target?.selectorConfig || '',
    crawlType: target?.crawlType || 'STATIC',
    enabled: target?.enabled ?? true,
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSubmit(formData);
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-2xl max-h-[90vh] overflow-y-auto">
        <div className="flex justify-between items-center p-4 border-b">
          <h2 className="text-xl font-semibold">
            {target ? '타겟 수정' : '새 타겟 추가'}
          </h2>
          <button onClick={onClose} className="p-1 hover:bg-gray-100 rounded">
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-4 space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              이름 *
            </label>
            <input
              type="text"
              value={formData.name}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              className="w-full border rounded-lg px-3 py-2"
              required
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              URL *
            </label>
            <input
              type="url"
              value={formData.url}
              onChange={(e) => setFormData({ ...formData, url: e.target.value })}
              className="w-full border rounded-lg px-3 py-2"
              placeholder="https://example.com/news"
              required
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              설명
            </label>
            <textarea
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              className="w-full border rounded-lg px-3 py-2"
              rows={2}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Cron 표현식 *
            </label>
            <input
              type="text"
              value={formData.cronExpression}
              onChange={(e) => setFormData({ ...formData, cronExpression: e.target.value })}
              className="w-full border rounded-lg px-3 py-2"
              placeholder="0 0 * * * * (매시 정각)"
              required
            />
            <p className="text-xs text-gray-500 mt-1">
              예: 0 0 * * * * (매시), 0 */30 * * * * (30분마다)
            </p>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              선택자 설정 (JSON)
            </label>
            <textarea
              value={formData.selectorConfig}
              onChange={(e) => setFormData({ ...formData, selectorConfig: e.target.value })}
              className="w-full border rounded-lg px-3 py-2 font-mono text-sm"
              rows={6}
              placeholder={`{
  "articleItemSelector": ".article-item",
  "titleSelector": "h2.title",
  "linkSelector": "a.link",
  "contentSelector": ".content",
  "dateSelector": ".date"
}`}
            />
          </div>

          <div className="flex gap-4">
            <div className="flex-1">
              <label className="block text-sm font-medium text-gray-700 mb-1">
                크롤링 타입
              </label>
              <select
                value={formData.crawlType}
                onChange={(e) =>
                  setFormData({ ...formData, crawlType: e.target.value as 'STATIC' | 'DYNAMIC' })
                }
                className="w-full border rounded-lg px-3 py-2"
              >
                <option value="STATIC">Static (Jsoup)</option>
                <option value="DYNAMIC">Dynamic (Selenium)</option>
              </select>
            </div>

            <div className="flex items-center">
              <label className="flex items-center gap-2 cursor-pointer">
                <input
                  type="checkbox"
                  checked={formData.enabled}
                  onChange={(e) => setFormData({ ...formData, enabled: e.target.checked })}
                  className="w-4 h-4 rounded"
                />
                <span className="text-sm font-medium text-gray-700">활성화</span>
              </label>
            </div>
          </div>

          <div className="flex justify-end gap-2 pt-4 border-t">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 border rounded-lg hover:bg-gray-50"
            >
              취소
            </button>
            <button
              type="submit"
              disabled={isLoading}
              className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50"
            >
              {isLoading ? '저장 중...' : '저장'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
