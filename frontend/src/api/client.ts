import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';

// Falls back to a same-origin relative path (inherits the page's own protocol) rather than
// a hardcoded http:// URL, so a misconfigured deployment never silently downgrades to plaintext.
const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api';

// Auth tokens are httpOnly cookies set by the server — never readable or stored by this app.
// withCredentials sends them automatically; withXSRFToken/xsrf* options make axios read the
// non-httpOnly XSRF-TOKEN cookie and echo it back as X-XSRF-TOKEN on mutating requests.
export const api = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  withCredentials: true,
  withXSRFToken: true,
  xsrfCookieName: 'XSRF-TOKEN',
  xsrfHeaderName: 'X-XSRF-TOKEN',
});

let isRefreshing = false;
let pendingQueue: Array<(ok: boolean) => void> = [];

function flushQueue(ok: boolean) {
  pendingQueue.forEach((cb) => cb(ok));
  pendingQueue = [];
}

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const original = error.config as InternalAxiosRequestConfig & { _retry?: boolean };
    const status = error.response?.status;
    const isAuthEndpoint = original?.url?.includes('/auth/');

    if (status === 401 && !original._retry && !isAuthEndpoint) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          pendingQueue.push((ok) => (ok ? resolve(api(original)) : reject(error)));
        });
      }

      original._retry = true;
      isRefreshing = true;
      try {
        // No body needed — the refresh_token cookie is sent automatically, and the server
        // responds with fresh Set-Cookie headers for both tokens.
        await axios.post(`${BASE_URL}/auth/refresh`, null, { withCredentials: true });
        flushQueue(true);
        return api(original);
      } catch (refreshErr) {
        flushQueue(false);
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
  // Best-effort — clears the httpOnly cookies server-side. Fire-and-forget since we're
  // navigating away regardless of whether this call succeeds.
  axios.post(`${BASE_URL}/auth/logout`, null, { withCredentials: true }).catch(() => {});
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
