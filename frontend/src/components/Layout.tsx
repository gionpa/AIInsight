import { Link, Outlet, useLocation } from 'react-router-dom';
import {
  LayoutDashboard,
  Target,
  Newspaper,
  History,
  FileText,
  Zap,
} from 'lucide-react';

const navItems = [
  { path: '/dashboard', label: '대시보드', icon: LayoutDashboard },
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
          <Link to="/" className="flex items-center gap-2 hover:opacity-80 transition-opacity">
            <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-cyan-400 to-purple-500 flex items-center justify-center">
              <Zap className="w-5 h-5 text-white" />
            </div>
            <div>
              <h1 className="text-lg font-bold">AI<span className="text-cyan-400">인사이트</span></h1>
            </div>
          </Link>
          <p className="text-gray-400 text-sm mt-2">AI 뉴스 수집 에이전트</p>
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
