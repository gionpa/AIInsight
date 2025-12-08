import axios from 'axios';
import type { AxiosError, InternalAxiosRequestConfig } from 'axios';

// Helper function to get cookie value
function getCookie(name: string): string | null {
  const value = `; ${document.cookie}`;
  const parts = value.split(`; ${name}=`);
  if (parts.length === 2) return parts.pop()?.split(';').shift() || null;
  return null;
}

// 항상 상대 경로 사용 (Vite 프록시 또는 프로덕션에서 동일 도메인)
const API_BASE_URL = '/api';

// 토큰 갱신 중인지 추적
let isRefreshing = false;
// 토큰 갱신 중 대기하는 요청들
let failedQueue: Array<{
  resolve: (value?: unknown) => void;
  reject: (reason?: unknown) => void;
}> = [];

const processQueue = (error: AxiosError | null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve();
    }
  });
  failedQueue = [];
};

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true, // 쿠키 전송 활성화
});

// Request interceptor: JWT 토큰을 Authorization 헤더에 추가
apiClient.interceptors.request.use(
  (config) => {
    const accessToken = getCookie('accessToken');
    if (accessToken) {
      config.headers.Authorization = `Bearer ${accessToken}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor: 401 에러 시 토큰 자동 갱신
apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

    // 401 에러이고, 재시도가 아니며, refresh 요청이 아닌 경우
    if (
      error.response?.status === 401 &&
      !originalRequest._retry &&
      !originalRequest.url?.includes('/auth/refresh') &&
      !originalRequest.url?.includes('/auth/me')
    ) {
      if (isRefreshing) {
        // 이미 갱신 중이면 대기열에 추가
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        }).then(() => apiClient(originalRequest));
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        // refresh token으로 새 access token 요청
        const response = await axios.post(
          `${API_BASE_URL}/auth/refresh`,
          {},
          { withCredentials: true }
        );

        if (response.status === 200) {
          // 쿠키는 서버에서 자동으로 설정됨
          // 대기 중인 요청들 처리
          processQueue(null);

          // 원래 요청 재시도
          return apiClient(originalRequest);
        }
      } catch (refreshError) {
        // 갱신 실패 시 대기 중인 요청들 에러 처리
        processQueue(refreshError as AxiosError);

        // 로그인 페이지로 리다이렉트 (refreshToken도 만료된 경우)
        if (window.location.pathname !== '/login') {
          window.location.href = '/login';
        }
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    console.error('API Error:', error.response?.data || error.message);
    return Promise.reject(error);
  }
);
