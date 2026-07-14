import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import type { TokenResponse } from '../types';

// Falls back to a same-origin relative path (inherits the page's own protocol) rather than
// a hardcoded http:// URL, so a misconfigured deployment never silently downgrades to plaintext.
const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api';

const ACCESS_KEY = 'cb_access_token';
const REFRESH_KEY = 'cb_refresh_token';

export const tokenStore = {
  getAccess: () => localStorage.getItem(ACCESS_KEY),
  getRefresh: () => localStorage.getItem(REFRESH_KEY),
  set: (access: string, refresh: string) => {
    localStorage.setItem(ACCESS_KEY, access);
    localStorage.setItem(REFRESH_KEY, refresh);
  },
  clear: () => {
    localStorage.removeItem(ACCESS_KEY);
    localStorage.removeItem(REFRESH_KEY);
  },
};

export const api = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' },
});

api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = tokenStore.getAccess();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

let isRefreshing = false;
let pendingQueue: Array<(token: string | null) => void> = [];

function flushQueue(token: string | null) {
  pendingQueue.forEach((cb) => cb(token));
  pendingQueue = [];
}

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const original = error.config as InternalAxiosRequestConfig & { _retry?: boolean };
    const status = error.response?.status;
    const isAuthEndpoint = original?.url?.includes('/auth/');

    if (status === 401 && !original._retry && !isAuthEndpoint) {
      const refreshToken = tokenStore.getRefresh();
      if (!refreshToken) {
        forceLogout();
        return Promise.reject(error);
      }

      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          pendingQueue.push((token) => {
            if (token) {
              original.headers.Authorization = `Bearer ${token}`;
              resolve(api(original));
            } else {
              reject(error);
            }
          });
        });
      }

      original._retry = true;
      isRefreshing = true;
      try {
        const { data } = await axios.post<TokenResponse>(`${BASE_URL}/auth/refresh`, {
          refreshToken,
        });
        tokenStore.set(data.accessToken!, data.refreshToken!);
        flushQueue(data.accessToken);
        original.headers.Authorization = `Bearer ${data.accessToken!}`;
        return api(original);
      } catch (refreshErr) {
        flushQueue(null);
        forceLogout();
        return Promise.reject(refreshErr);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);

function forceLogout() {
  tokenStore.clear();
  if (window.location.pathname !== '/login') {
    window.location.href = '/login';
  }
}

export async function extractBlobError(err: unknown): Promise<string> {
  if (err && typeof err === 'object' && 'response' in err) {
    const resp = (err as { response?: { data?: unknown; status?: number } }).response;
    if (resp?.data instanceof Blob) {
      try {
        const text = await resp.data.text();
        const json = JSON.parse(text) as { message?: string };
        if (json.message) return json.message;
      } catch { /* ignore */ }
      return `Server error (${resp.status ?? 500})`;
    }
  }
  return extractErrorMessage(err);
}

export function extractErrorMessage(err: unknown): string {
  if (axios.isAxiosError(err)) {
    const raw = err.response?.data;
    // blob responses (file downloads) return error body as Blob — parse it
    if (raw instanceof Blob && raw.type.includes('json')) {
      // async parse not possible here; return generic message
      return `Server error (${err.response?.status ?? 500}) — check backend logs`;
    }
    const data = raw as { message?: string; fieldErrors?: Record<string, string> } | undefined;
    if (data?.fieldErrors) {
      return Object.values(data.fieldErrors).join(', ');
    }
    if (data?.message) {
      return data.message;
    }
    return err.message;
  }
  return 'An unexpected error occurred';
}

export { BASE_URL };
