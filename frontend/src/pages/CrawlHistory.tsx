import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getTodayCrawlHistory, getAllCrawlTargets, getCrawlHistoryByTarget } from '../api';
import { CheckCircle, XCircle, AlertCircle, Clock } from 'lucide-react';

export default function CrawlHistory() {
  const [selectedTarget, setSelectedTarget] = useState<number | null>(null);
  const [page, setPage] = useState(0);

  const { data: targets } = useQuery({
    queryKey: ['crawlTargetsAll'],
    queryFn: getAllCrawlTargets,
  });

  const { data: todayHistory, isLoading: isLoadingToday } = useQuery({
    queryKey: ['todayCrawlHistory'],
    queryFn: getTodayCrawlHistory,
    enabled: selectedTarget === null,
  });

  const { data: targetHistory, isLoading: isLoadingTarget } = useQuery({
    queryKey: ['targetCrawlHistory', selectedTarget, page],
    queryFn: () => getCrawlHistoryByTarget(selectedTarget!, page, 20),
    enabled: selectedTarget !== null,
  });

  const isLoading = selectedTarget === null ? isLoadingToday : isLoadingTarget;
  const historyData = selectedTarget === null ? todayHistory : targetHistory?.content;

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
          <option value="">오늘 전체</option>
          {targets?.map((target) => (
            <option key={target.id} value={target.id}>
              {target.name}
            </option>
          ))}
        </select>
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
                    {new Date(history.executedAt).toLocaleString()}
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

          {/* Pagination for target-specific history */}
          {selectedTarget !== null && targetHistory && targetHistory.totalPages > 1 && (
            <div className="px-4 py-3 bg-gray-50 flex justify-between items-center">
              <button
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={targetHistory.first}
                className="px-3 py-1 bg-white border rounded disabled:opacity-50"
              >
                이전
              </button>
              <span className="text-sm text-gray-500">
                {targetHistory.number + 1} / {targetHistory.totalPages} 페이지
              </span>
              <button
                onClick={() => setPage((p) => p + 1)}
                disabled={targetHistory.last}
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
