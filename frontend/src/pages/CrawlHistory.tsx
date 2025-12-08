import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getAllCrawlHistory, getAllCrawlTargets, getCrawlHistoryByTarget, deleteOldCrawlHistory } from '../api';
import { CheckCircle, XCircle, AlertCircle, Clock, Trash2 } from 'lucide-react';

export default function CrawlHistory() {
  const [selectedTarget, setSelectedTarget] = useState<number | null>(null);
  const [page, setPage] = useState(0);
  const queryClient = useQueryClient();

  const { data: targets } = useQuery({
    queryKey: ['crawlTargetsAll'],
    queryFn: getAllCrawlTargets,
  });

  // 전체 이력 페이징 조회 (기본)
  const { data: allHistory, isLoading: isLoadingAll } = useQuery({
    queryKey: ['allCrawlHistory', page],
    queryFn: () => getAllCrawlHistory(page, 30),
    enabled: selectedTarget === null,
  });

  // 타겟별 이력 조회
  const { data: targetHistory, isLoading: isLoadingTarget } = useQuery({
    queryKey: ['targetCrawlHistory', selectedTarget, page],
    queryFn: () => getCrawlHistoryByTarget(selectedTarget!, page, 30),
    enabled: selectedTarget !== null,
  });

  // 한달 이전 이력 삭제
  const deleteOldMutation = useMutation({
    mutationFn: deleteOldCrawlHistory,
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['allCrawlHistory'] });
      queryClient.invalidateQueries({ queryKey: ['targetCrawlHistory'] });
      alert(`${data.deletedCount}건의 이전 이력이 삭제되었습니다.`);
    },
    onError: () => {
      alert('삭제 중 오류가 발생했습니다.');
    },
  });

  const isLoading = selectedTarget === null ? isLoadingAll : isLoadingTarget;
  const pageData = selectedTarget === null ? allHistory : targetHistory;
  const historyData = pageData?.content;

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'SUCCESS':
        return <CheckCircle className="w-5 h-5 text-green-500" />;
      case 'FAILED':
        return <XCircle className="w-5 h-5 text-red-500" />;
      case 'PARTIAL':
        return <AlertCircle className="w-5 h-5 text-yellow-500" />;
      default:
        return <Clock className="w-5 h-5 text-gray-500" />;
    }
  };

  const getStatusClass = (status: string) => {
    switch (status) {
      case 'SUCCESS':
        return 'bg-green-100 text-green-700';
      case 'FAILED':
        return 'bg-red-100 text-red-700';
      case 'PARTIAL':
        return 'bg-yellow-100 text-yellow-700';
      default:
        return 'bg-gray-100 text-gray-700';
    }
  };

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">크롤링 이력</h1>

        <div className="flex gap-3 items-center">
          {/* 한달 이전 삭제 버튼 */}
          <button
            onClick={() => {
              if (confirm('한달이 지난 이력을 삭제하시겠습니까?')) {
                deleteOldMutation.mutate();
              }
            }}
            disabled={deleteOldMutation.isPending}
            className="flex items-center gap-2 px-3 py-2 text-sm bg-red-50 text-red-600 hover:bg-red-100 rounded-lg border border-red-200 disabled:opacity-50"
          >
            <Trash2 className="w-4 h-4" />
            한달 이전 삭제
          </button>

          {/* Target Filter */}
          <select
            value={selectedTarget ?? ''}
            onChange={(e) => {
              const value = e.target.value;
              setSelectedTarget(value ? Number(value) : null);
              setPage(0);
            }}
            className="border rounded-lg px-4 py-2"
          >
            <option value="">전체</option>
            {targets?.map((target) => (
              <option key={target.id} value={target.id}>
                {target.name}
              </option>
            ))}
          </select>
        </div>
      </div>

      {isLoading ? (
        <div className="flex items-center justify-center h-64">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
        </div>
      ) : (
        <div className="bg-white rounded-lg shadow overflow-hidden">
          <table className="w-full">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-4 py-3 text-left text-sm font-medium text-gray-500">상태</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-gray-500">타겟</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-gray-500">발견</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-gray-500">신규</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-gray-500">소요시간</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-gray-500">실행시간</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-gray-500">에러</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {historyData?.map((history) => (
                <tr key={history.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-2">
                      {getStatusIcon(history.status)}
                      <span className={`px-2 py-0.5 rounded text-xs font-medium ${getStatusClass(history.status)}`}>
                        {history.status}
                      </span>
                    </div>
                  </td>
                  <td className="px-4 py-3 font-medium">{history.targetName}</td>
                  <td className="px-4 py-3">{history.articlesFound}</td>
                  <td className="px-4 py-3">
                    <span className={history.articlesNew > 0 ? 'text-green-600 font-medium' : ''}>
                      {history.articlesNew}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-gray-500">
                    {history.durationMs ? `${(history.durationMs / 1000).toFixed(2)}s` : '-'}
                  </td>
                  <td className="px-4 py-3 text-gray-500">
                    {new Date(history.executedAt).toLocaleString('ko-KR', {
                      timeZone: 'Asia/Seoul',
                      year: 'numeric',
                      month: '2-digit',
                      day: '2-digit',
                      hour: '2-digit',
                      minute: '2-digit',
                    })}
                  </td>
                  <td className="px-4 py-3">
                    {history.errorMessage && (
                      <span className="text-red-600 text-sm truncate block max-w-xs" title={history.errorMessage}>
                        {history.errorMessage}
                      </span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          {(!historyData || historyData.length === 0) && (
            <div className="text-center py-12 text-gray-500">
              크롤링 이력이 없습니다.
            </div>
          )}

          {/* Pagination */}
          {pageData && pageData.totalPages > 1 && (
            <div className="px-4 py-3 bg-gray-50 flex justify-between items-center">
              <button
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={pageData.first}
                className="px-3 py-1 bg-white border rounded disabled:opacity-50"
              >
                이전
              </button>
              <span className="text-sm text-gray-500">
                {pageData.number + 1} / {pageData.totalPages} 페이지 (총 {pageData.totalElements}건)
              </span>
              <button
                onClick={() => setPage((p) => p + 1)}
                disabled={pageData.last}
                className="px-3 py-1 bg-white border rounded disabled:opacity-50"
              >
                다음
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
