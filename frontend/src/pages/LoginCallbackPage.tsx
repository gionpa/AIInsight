import { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

// Helper function to set cookie
function setCookie(name: string, value: string, days: number) {
  const expires = new Date();
  expires.setTime(expires.getTime() + days * 24 * 60 * 60 * 1000);
  document.cookie = `${name}=${value};expires=${expires.toUTCString()};path=/`;
}

export default function LoginCallbackPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { checkAuth } = useAuth();
  const success = searchParams.get('success');
  const accessToken = searchParams.get('accessToken');
  const refreshToken = searchParams.get('refreshToken');

  useEffect(() => {
    const handleCallback = async () => {
      if (success === 'true' && accessToken && refreshToken) {
        // Store tokens in cookies (for cross-port development)
        setCookie('accessToken', accessToken, 1); // 1 day
        setCookie('refreshToken', refreshToken, 7); // 7 days

        // Clear URL params for security (tokens shouldn't stay in URL)
        window.history.replaceState({}, document.title, '/login/callback');

        await checkAuth();
        navigate('/dashboard');
      } else if (success === 'true') {
        // Fallback: tokens might already be in cookies (same-origin)
        await checkAuth();
        navigate('/dashboard');
      } else {
        navigate('/login?error=true');
      }
    };

    handleCallback();
  }, [success, accessToken, refreshToken, navigate, checkAuth]);

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-100">
      <div className="text-center">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
        <p className="mt-4 text-gray-600">로그인 처리 중...</p>
      </div>
    </div>
  );
}
