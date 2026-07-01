import { api, BASE_URL, tokenStore } from './client';
import type {
  ApprovalRouting,
  Booking,
  CurrentUser,
  PageResponse,
  Party,
  Permission,
  ReportSummary,
  Role,
  TokenResponse,
  UserAccount,
} from '../types';

// ---------- Auth ----------
export const authApi = {
  login: (username: string, password: string) =>
    api.post<TokenResponse>('/auth/login', { username, password }).then((r) => r.data),
  me: () => api.get<CurrentUser>('/auth/me').then((r) => r.data),
};

// ---------- Admin ----------
export const adminApi = {
  listPermissions: () => api.get<Permission[]>('/admin/permissions').then((r) => r.data),

  listRoles: (page = 0, size = 100) =>
    api.get<PageResponse<Role>>('/admin/roles', { params: { page, size } }).then((r) => r.data),
  createRole: (body: Partial<Role> & { permissionIds: number[] }) =>
    api.post<Role>('/admin/roles', body).then((r) => r.data),
  updateRole: (id: number, body: Partial<Role> & { permissionIds: number[] }) =>
    api.put<Role>(`/admin/roles/${id}`, body).then((r) => r.data),
  deleteRole: (id: number) => api.delete(`/admin/roles/${id}`),

  listUsers: (search: string, page = 0, size = 20) =>
    api
      .get<PageResponse<UserAccount>>('/admin/users', { params: { search: search || undefined, page, size } })
      .then((r) => r.data),
  createUser: (body: Record<string, unknown>) =>
    api.post<UserAccount>('/admin/users', body).then((r) => r.data),
  updateUser: (id: number, body: Record<string, unknown>) =>
    api.put<UserAccount>(`/admin/users/${id}`, body).then((r) => r.data),
  deleteUser: (id: number) => api.delete(`/admin/users/${id}`),

  listApprovalRouting: () => api.get<ApprovalRouting[]>('/admin/approval-routing').then((r) => r.data),
  createApprovalRouting: (body: { roleId: number | null; userId: number | null; creatorRoleId: number | null; active: boolean }) =>
    api.post<ApprovalRouting>('/admin/approval-routing', body).then((r) => r.data),
  deleteApprovalRouting: (id: number) => api.delete(`/admin/approval-routing/${id}`),
};

// ---------- Master (parties) ----------
export const partyApi = {
  list: (params: { name?: string; city?: string; pincode?: string; page?: number; size?: number }) =>
    api.get<PageResponse<Party>>('/master/parties', { params }).then((r) => r.data),
  listActive: () => api.get<Party[]>('/master/parties/active').then((r) => r.data),
  get: (id: number) => api.get<Party>(`/master/parties/${id}`).then((r) => r.data),
  create: (body: Record<string, unknown>) => api.post<Party>('/master/parties', body).then((r) => r.data),
  update: (id: number, body: Record<string, unknown>) =>
    api.put<Party>(`/master/parties/${id}`, body).then((r) => r.data),
  remove: (id: number) => api.delete(`/master/parties/${id}`),
};

// ---------- Bookings ----------
export const bookingApi = {
  search: (params: Record<string, unknown>) =>
    api.get<PageResponse<Booking>>('/bookings', { params }).then((r) => r.data),
  get: (id: number) => api.get<Booking>(`/bookings/${id}`).then((r) => r.data),
  create: (body: Record<string, unknown>) => api.post<Booking>('/bookings', body).then((r) => r.data),
  update: (id: number, body: Record<string, unknown>) =>
    api.put<Booking>(`/bookings/${id}`, body).then((r) => r.data),
  remove: (id: number) => api.delete(`/bookings/${id}`),
  submit: (id: number) => api.post<Booking>(`/bookings/${id}/submit`).then((r) => r.data),
  approve: (id: number, remarks: string) =>
    api.post<Booking>(`/bookings/${id}/approve`, { remarks }).then((r) => r.data),
  reject: (id: number, remarks: string) =>
    api.post<Booking>(`/bookings/${id}/reject`, { remarks }).then((r) => r.data),
  changeStatus: (id: number, status: string) =>
    api.post<Booking>(`/bookings/${id}/status`, { status }).then((r) => r.data),
  stickerUrl: (id: number) => `${BASE_URL}/bookings/${id}/sticker`,
  fetchSticker: (id: number) =>
    api.get(`/bookings/${id}/sticker`, { responseType: 'blob' }).then((r) => r.data as Blob),
};

// ---------- Reports ----------
export const reportApi = {
  summary: (params: { granularity: string; from?: string; to?: string }) =>
    api.get<ReportSummary>('/reports/summary', { params }).then((r) => r.data),
  exportExcel: (params: { granularity: string; from?: string; to?: string }) =>
    api.get('/reports/export', { params, responseType: 'blob' }).then((r) => r.data as Blob),
};

export { tokenStore };
