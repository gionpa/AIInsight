import { Link, Outlet, useLocation } from 'react-router-dom';
import {
  LayoutDashboard,
  Target,
  Newspaper,
  History,
  RefreshCw,
  FileText,
} from 'lucide-react';

const navItems = [
  { path: '/', label: '대시보드', icon: LayoutDashboard },
  { path: '/report', label: '리포트', icon: FileText },
  { path: '/targets', label: '크롤링 타겟', icon: Target },
  { path: '/articles', label: '뉴스 기사', icon: Newspaper },
  { path: '/history', label: '크롤링 이력', icon: History },
];

export default function Layout() {
  const location = useLocation();

  return (
    <div className="min-h-screen flex">
      {/* Sidebar */}
      <aside className="w-64 bg-gray-900 text-white">
        <div className="p-4">
          <h1 className="text-xl font-bold flex items-center gap-2">
            <RefreshCw className="w-6 h-6" />
            AI Insight
          </h1>
          <p className="text-gray-400 text-sm mt-1">AI 뉴스 수집 에이전트</p>
        </div>
        <nav className="mt-4">
          {navItems.map((item) => {
            const Icon = item.icon;
            const isActive = location.pathname === item.path;
            return (
              <Link
                key={item.path}
                to={item.path}
                className={`flex items-center gap-3 px-4 py-3 hover:bg-gray-800 transition-colors ${
                  isActive ? 'bg-gray-800 border-l-4 border-blue-500' : ''
                }`}
              >
                <Icon className="w-5 h-5" />
                {item.label}
              </Link>
            );
          })}
        </nav>
      </aside>

      {/* Main Content */}
      <main className="flex-1 p-6 overflow-auto">
        <Outlet />
      </main>
    </div>
  );
}
