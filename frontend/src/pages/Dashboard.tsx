import { useQuery } from '@tanstack/react-query';
import { getDashboardStats } from '../api';
import {
  Target,
  Newspaper,
  CheckCircle,
  XCircle,
  TrendingUp,
  Clock,
} from 'lucide-react';

export default function Dashboard() {
  const { data: stats, isLoading, error } = useQuery({
    queryKey: ['dashboard'],
    queryFn: getDashboardStats,
    refetchInterval: 30000, // 30초마다 갱신
  });

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-red-50 text-red-600 p-4 rounded-lg">
        데이터를 불러오는데 실패했습니다.
      </div>
    );
  }

  const statCards = [
    {
      label: '활성 타겟',
      value: `${stats?.activeTargets || 0} / ${stats?.totalTargets || 0}`,
      icon: Target,
      color: 'bg-blue-500',
    },
    {
      label: '전체 기사',
      value: stats?.totalArticles || 0,
      icon: Newspaper,
      color: 'bg-green-500',
    },
    {
      label: '신규 기사',
      value: stats?.newArticles || 0,
      icon: TrendingUp,
      color: 'bg-yellow-500',
    },
    {
      label: '오늘 수집',
      value: stats?.todayCrawled || 0,
      icon: Clock,
      color: 'bg-purple-500',
    },
    {
      label: '성공 크롤링',
      value: stats?.successfulCrawls || 0,
      icon: CheckCircle,
      color: 'bg-emerald-500',
    },
    {
      label: '실패 크롤링',
      value: stats?.failedCrawls || 0,
      icon: XCircle,
      color: 'bg-red-500',
    },
  ];

  return (
    <div>
      <h1 className="text-2xl font-bold mb-6">대시보드</h1>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-4 mb-8">
        {statCards.map((card) => {
          const Icon = card.icon;
          return (
            <div
              key={card.label}
              className="bg-white rounded-lg shadow p-4 flex items-center gap-4"
            >
              <div className={`${card.color} p-3 rounded-lg`}>
                <Icon className="w-6 h-6 text-white" />
              </div>
              <div>
                <p className="text-gray-500 text-sm">{card.label}</p>
                <p className="text-xl font-bold">{card.value}</p>
              </div>
            </div>
          );
        })}
      </div>

      {/* Category Distribution */}
      {stats?.categoryDistribution && Object.keys(stats.categoryDistribution).length > 0 && (
        <div className="bg-white rounded-lg shadow p-6 mb-8">
          <h2 className="text-lg font-semibold mb-4">카테고리별 분포 (최근 7일)</h2>
          <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-6 gap-4">
            {Object.entries(stats.categoryDistribution).map(([category, count]) => (
              <div key={category} className="text-center p-3 bg-gray-50 rounded-lg">
                <p className="text-xs text-gray-500">{category}</p>
                <p className="text-lg font-bold">{count}</p>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Recent Crawls */}
      <div className="bg-white rounded-lg shadow p-6">
        <h2 className="text-lg font-semibold mb-4">최근 크롤링</h2>
        {stats?.recentCrawls && stats.recentCrawls.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-4 py-2 text-left text-sm font-medium text-gray-500">타겟</th>
                  <th className="px-4 py-2 text-left text-sm font-medium text-gray-500">상태</th>
                  <th className="px-4 py-2 text-left text-sm font-medium text-gray-500">신규 기사</th>
                  <th className="px-4 py-2 text-left text-sm font-medium text-gray-500">시간</th>
                </tr>
              </thead>
              <tbody className="divide-y">
                {stats.recentCrawls.map((crawl, index) => (
                  <tr key={index} className="hover:bg-gray-50">
                    <td className="px-4 py-3">{crawl.targetName}</td>
                    <td className="px-4 py-3">
                      <span
                        className={`px-2 py-1 rounded text-xs font-medium ${
                          crawl.status === 'SUCCESS'
                            ? 'bg-green-100 text-green-700'
                            : crawl.status === 'FAILED'
                            ? 'bg-red-100 text-red-700'
                            : 'bg-yellow-100 text-yellow-700'
                        }`}
                      >
                        {crawl.status}
                      </span>
                    </td>
                    <td className="px-4 py-3">{crawl.articlesNew}</td>
                    <td className="px-4 py-3 text-gray-500">{crawl.executedAt}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <p className="text-gray-500">오늘 크롤링 기록이 없습니다.</p>
        )}
      </div>
    </div>
  );
}
