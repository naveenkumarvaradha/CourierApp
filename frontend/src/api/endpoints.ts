import { api, BASE_URL, tokenStore } from './client';
import type { MfaSetupResponse } from '../types';
import type {
  ApprovalInfo,
  ApprovalRouting,
  AuditLog,
  Booking,
  Company,
  DashboardTasks,
  CompanySettings,
  CourierWay,
  CurrentUser,
  Department,
  FlexFieldDefinition,
  FlexFieldValues,
  MailConfig,
  PackageType,
  PageResponse,
  Party,
  PasswordPolicy,
  Permission,
  ReportSchedule,
  ReportSummary,
  Role,
  StickerField,
  TokenResponse,
  UserAccount,
} from '../types';

// ---------- Dashboard ----------
export const dashboardApi = {
  getTasks: () => api.get<DashboardTasks>('/dashboard').then((r) => r.data),
};

// ---------- Approval info ----------
export const approvalApi = {
  bookingInfo: (id: number) =>
    api.get<ApprovalInfo>(`/bookings/${id}/approval-info`).then((r) => r.data),
  partyInfo: (id: number) =>
    api.get<ApprovalInfo>(`/master/parties/${id}/approval-info`).then((r) => r.data),
};

// ---------- Auth ----------
export const authApi = {
  listCompanies: () => api.get<Company[]>('/auth/companies').then((r) => r.data),
  login: (companyCode: string, username: string, password: string) =>
    api.post<TokenResponse>('/auth/login', { companyCode, username, password }).then((r) => r.data),
  me: () => api.get<CurrentUser>('/auth/me').then((r) => r.data),
  changePassword: (currentPassword: string, newPassword: string) =>
    api.post<{ message: string }>('/auth/change-password', { currentPassword, newPassword }).then((r) => r.data),
  forgotPassword: (username: string) =>
    api.post<{ message: string }>('/auth/forgot-password', { username }).then((r) => r.data),
  resetPassword: (token: string, newPassword: string) =>
    api.post<{ message: string }>('/auth/reset-password', { token, newPassword }).then((r) => r.data),
  logout: () =>
    api.post<{ message: string }>('/auth/logout').then((r) => r.data),
  setupMfa: () =>
    api.post<MfaSetupResponse>('/auth/setup-mfa').then((r) => r.data),
  enableMfa: (code: string) =>
    api.post<{ message: string }>('/auth/enable-mfa', { code }).then((r) => r.data),
  disableMfa: () =>
    api.post<{ message: string }>('/auth/disable-mfa').then((r) => r.data),
  confirmMfa: (mfaPendingToken: string, code: string) =>
    api.post<import('../types').TokenResponse>('/auth/confirm-mfa', { mfaPendingToken, code }).then((r) => r.data),
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
  getUserHistory: (id: number, page = 0, size = 20) =>
    api.get<PageResponse<import('../types').AuditLog>>(`/admin/users/${id}/history`, { params: { page, size } }).then((r) => r.data),

  listApprovalRouting: () => api.get<ApprovalRouting[]>('/admin/approval-routing').then((r) => r.data),
  createApprovalRouting: (body: { roleId: number | null; userId: number | null; creatorRoleId: number | null; creatorUserId: number | null; active: boolean; module?: string; level?: number }) =>
    api.post<ApprovalRouting>('/admin/approval-routing', body).then((r) => r.data),
  deleteApprovalRouting: (id: number) => api.delete(`/admin/approval-routing/${id}`),

  getCompanySettings: () => api.get<CompanySettings>('/admin/company-settings').then((r) => r.data),
  updateCompanySettings: (body: Record<string, unknown>) =>
    api.put<CompanySettings>('/admin/company-settings', body).then((r) => r.data),

  listCourierWays: () => api.get<CourierWay[]>('/admin/courier-ways').then((r) => r.data),
  listActiveCourierWays: () => api.get<CourierWay[]>('/admin/courier-ways/active').then((r) => r.data),
  createCourierWay: (body: { name: string; active: boolean }) =>
    api.post<CourierWay>('/admin/courier-ways', body).then((r) => r.data),
  updateCourierWay: (id: number, body: { name: string; active: boolean }) =>
    api.put<CourierWay>(`/admin/courier-ways/${id}`, body).then((r) => r.data),
  deleteCourierWay: (id: number) => api.delete(`/admin/courier-ways/${id}`),

  listPackageTypes: () => api.get<PackageType[]>('/admin/package-types').then((r) => r.data),
  listActivePackageTypes: () => api.get<PackageType[]>('/admin/package-types/active').then((r) => r.data),
  createPackageType: (body: { name: string; active: boolean }) =>
    api.post<PackageType>('/admin/package-types', body).then((r) => r.data),
  updatePackageType: (id: number, body: { name: string; active: boolean }) =>
    api.put<PackageType>(`/admin/package-types/${id}`, body).then((r) => r.data),
  deletePackageType: (id: number) => api.delete(`/admin/package-types/${id}`),

  listDepartments: () => api.get<Department[]>('/admin/departments').then((r) => r.data),
  listActiveDepartments: () => api.get<Department[]>('/admin/departments/active').then((r) => r.data),
  createDepartment: (body: { name: string; active: boolean }) =>
    api.post<Department>('/admin/departments', body).then((r) => r.data),
  updateDepartment: (id: number, body: { name: string; active: boolean }) =>
    api.put<Department>(`/admin/departments/${id}`, body).then((r) => r.data),
  deleteDepartment: (id: number) => api.delete(`/admin/departments/${id}`),

  listCompanies: () => api.get<Company[]>('/admin/companies').then((r) => r.data),
  listActiveCompanies: () => api.get<Company[]>('/admin/companies/active').then((r) => r.data),
  createCompany: (body: { companyCode: string; name: string; active: boolean }) =>
    api.post<Company>('/admin/companies', body).then((r) => r.data),
  updateCompany: (id: number, body: { companyCode: string; name: string; active: boolean }) =>
    api.put<Company>(`/admin/companies/${id}`, body).then((r) => r.data),
  deleteCompany: (id: number) => api.delete(`/admin/companies/${id}`),
  getCompanySettingsById: (id: number) =>
    api.get<CompanySettings>(`/admin/companies/${id}/settings`).then((r) => r.data),
  updateCompanySettingsById: (id: number, body: Record<string, unknown>) =>
    api.put<CompanySettings>(`/admin/companies/${id}/settings`, body).then((r) => r.data),

  // Password policy
  getPasswordPolicy: () => api.get<PasswordPolicy>('/admin/password-policy').then((r) => r.data),
  updatePasswordPolicy: (body: Record<string, unknown>) =>
    api.put<PasswordPolicy>('/admin/password-policy', body).then((r) => r.data),

  // Sticker field config
  getStickerConfig: (companyId: number) =>
    api.get<StickerField[]>(`/admin/companies/${companyId}/sticker-config`).then((r) => r.data),
  saveStickerConfig: (companyId: number, fields: StickerField[]) =>
    api.put<StickerField[]>(`/admin/companies/${companyId}/sticker-config`, fields).then((r) => r.data),

  // Report schedules
  listReportSchedules: () => api.get<ReportSchedule[]>('/admin/report-schedules').then((r) => r.data),
  createReportSchedule: (body: Record<string, unknown>) =>
    api.post<ReportSchedule>('/admin/report-schedules', body).then((r) => r.data),
  updateReportSchedule: (id: number, body: Record<string, unknown>) =>
    api.put<ReportSchedule>(`/admin/report-schedules/${id}`, body).then((r) => r.data),
  deleteReportSchedule: (id: number) => api.delete(`/admin/report-schedules/${id}`),

  searchAuditLogs: (params: {
    module?: string; action?: string; performedBy?: string;
    fromDate?: string; toDate?: string; page?: number; size?: number;
  }) => api.get<PageResponse<AuditLog>>('/admin/audit-logs', { params }).then((r) => r.data),

  // Mail configuration
  getMailConfig: () => api.get<MailConfig>('/admin/mail-config').then((r) => r.data),
  saveMailConfig: (body: Record<string, unknown>) =>
    api.put<MailConfig>('/admin/mail-config', body).then((r) => r.data),
  testMailConfig: (email: string, smtpParams?: {
    smtpHost?: string; smtpPort?: string; smtpUsername?: string;
    smtpPassword?: string; smtpFromName?: string; smtpTls?: boolean;
  }) =>
    api.post<{ message: string }>('/admin/mail-config/test', { email, ...smtpParams }).then((r) => r.data),

  listFlexFields: (module?: string) =>
    api.get<FlexFieldDefinition[]>('/admin/flex-fields', { params: module ? { module } : {} }).then((r) => r.data),
  createFlexField: (body: Record<string, unknown>) =>
    api.post<FlexFieldDefinition>('/admin/flex-fields', body).then((r) => r.data),
  updateFlexField: (id: number, body: Record<string, unknown>) =>
    api.put<FlexFieldDefinition>(`/admin/flex-fields/${id}`, body).then((r) => r.data),
  deleteFlexField: (id: number) => api.delete(`/admin/flex-fields/${id}`),
  addFlexFieldOption: (fieldId: number, body: { optionValue: string; sortOrder: number; active: boolean }) =>
    api.post(`/admin/flex-fields/${fieldId}/options`, body).then((r) => r.data),
  deleteFlexFieldOption: (fieldId: number, optionId: number) =>
    api.delete(`/admin/flex-fields/${fieldId}/options/${optionId}`),
};

// ---------- Flex Field Values ----------
export const flexFieldApi = {
  getActiveFields: (module: string) =>
    api.get<FlexFieldDefinition[]>('/flex-fields/active', { params: { module } }).then((r) => r.data),
  getValues: (module: string, entityId: number) =>
    api.get<{ values: FlexFieldValues }>(`/flex-field-values/${module}/${entityId}`).then((r) => r.data),
  saveValues: (module: string, entityId: number, values: FlexFieldValues) =>
    api.post<{ values: FlexFieldValues }>(`/flex-field-values/${module}/${entityId}`, { values }).then((r) => r.data),
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
  approve: (id: number) => api.post<Party>(`/master/parties/${id}/approve`).then((r) => r.data),
  reject: (id: number, remarks?: string) =>
    api.post<Party>(`/master/parties/${id}/reject`, null, { params: remarks ? { remarks } : {} }).then((r) => r.data),
};

// ---------- Bookings ----------
export const bookingApi = {
  search: (params: Record<string, string | number | boolean | undefined | null>) =>
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
  updateAwb: (id: number, awbNumber: string) =>
    api.put<Booking>(`/bookings/${id}/awb`, { awbNumber }).then((r) => r.data),
  stickerUrl: (id: number) => `${BASE_URL}/bookings/${id}/sticker`,
  fetchSticker: (id: number) =>
    api.get(`/bookings/${id}/sticker`, { responseType: 'blob' }).then((r) => r.data as Blob),
  revise: (id: number) => api.post<Booking>(`/bookings/${id}/revise`).then((r) => r.data),
  requestCancellation: (id: number, remarks?: string) =>
    api.post<Booking>(`/bookings/${id}/request-cancellation`, null, { params: remarks ? { remarks } : {} }).then((r) => r.data),
  approveCancellation: (id: number) =>
    api.post<Booking>(`/bookings/${id}/approve-cancellation`).then((r) => r.data),
  rejectCancellation: (id: number) =>
    api.post<Booking>(`/bookings/${id}/reject-cancellation`).then((r) => r.data),
};

// ---------- Reports ----------
export const reportApi = {
  summary: (params: { granularity: string; from?: string; to?: string }) =>
    api.get<ReportSummary>('/reports/summary', { params }).then((r) => r.data),
  exportExcel: (params: { granularity: string; from?: string; to?: string }) =>
    api.get('/reports/export', { params, responseType: 'blob' }).then((r) => r.data as Blob),

  // Booking detail
  bookingDetailExcel: (from: string, to: string, status?: string) =>
    api.get('/reports/bookings/detail/excel', { params: { from, to, status: status || undefined }, responseType: 'blob' }).then((r) => r.data as Blob),
  bookingDetailPdf: (from: string, to: string, status?: string) =>
    api.get('/reports/bookings/detail/pdf', { params: { from, to, status: status || undefined }, responseType: 'blob' }).then((r) => r.data as Blob),

  // User Creation
  userCreationExcel: (from: string, to: string) =>
    api.get('/reports/users/creation/excel', { params: { from, to }, responseType: 'blob' }).then((r) => r.data as Blob),
  userCreationPdf: (from: string, to: string) =>
    api.get('/reports/users/creation/pdf', { params: { from, to }, responseType: 'blob' }).then((r) => r.data as Blob),

  // User Inactive
  userInactiveExcel: (from: string, to: string) =>
    api.get('/reports/users/inactive/excel', { params: { from, to }, responseType: 'blob' }).then((r) => r.data as Blob),
  userInactivePdf: (from: string, to: string) =>
    api.get('/reports/users/inactive/pdf', { params: { from, to }, responseType: 'blob' }).then((r) => r.data as Blob),

  // Party / Master
  partyExcel: (from: string, to: string) =>
    api.get('/reports/parties/excel', { params: { from, to }, responseType: 'blob' }).then((r) => r.data as Blob),
  partyPdf: (from: string, to: string) =>
    api.get('/reports/parties/pdf', { params: { from, to }, responseType: 'blob' }).then((r) => r.data as Blob),
};

export { tokenStore };
