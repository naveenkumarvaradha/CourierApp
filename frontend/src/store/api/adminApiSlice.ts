import { baseApi } from './baseApi';
import type {
  CourierWay, PackageType, Department, Role, Permission,
  UserAccount, ApprovalRouting, CompanySettings, PageResponse, AuditLog, UserMfaStatus,
} from '../../types';

export const adminApiSlice = baseApi.injectEndpoints({
  endpoints: (build) => ({
    // ── Courier Ways ──────────────────────────────────────────────────────────
    listCourierWays: build.query<CourierWay[], void>({
      query: () => ({ url: '/admin/courier-ways' }),
      providesTags: ['CourierWay'],
    }),
    listActiveCourierWays: build.query<CourierWay[], void>({
      query: () => ({ url: '/admin/courier-ways/active' }),
      providesTags: ['CourierWay'],
    }),
    createCourierWay: build.mutation<CourierWay, { name: string; active: boolean }>({
      query: (data) => ({ url: '/admin/courier-ways', method: 'POST', data }),
      invalidatesTags: ['CourierWay'],
    }),
    updateCourierWay: build.mutation<CourierWay, { id: number; name: string; active: boolean }>({
      query: ({ id, ...data }) => ({ url: `/admin/courier-ways/${id}`, method: 'PUT', data }),
      invalidatesTags: ['CourierWay'],
    }),
    deleteCourierWay: build.mutation<void, number>({
      query: (id) => ({ url: `/admin/courier-ways/${id}`, method: 'DELETE' }),
      invalidatesTags: ['CourierWay'],
    }),

    // ── Package Types ─────────────────────────────────────────────────────────
    listPackageTypes: build.query<PackageType[], void>({
      query: () => ({ url: '/admin/package-types' }),
      providesTags: ['PackageType'],
    }),
    listActivePackageTypes: build.query<PackageType[], void>({
      query: () => ({ url: '/admin/package-types/active' }),
      providesTags: ['PackageType'],
    }),
    createPackageType: build.mutation<PackageType, { name: string; active: boolean }>({
      query: (data) => ({ url: '/admin/package-types', method: 'POST', data }),
      invalidatesTags: ['PackageType'],
    }),
    updatePackageType: build.mutation<PackageType, { id: number; name: string; active: boolean }>({
      query: ({ id, ...data }) => ({ url: `/admin/package-types/${id}`, method: 'PUT', data }),
      invalidatesTags: ['PackageType'],
    }),
    deletePackageType: build.mutation<void, number>({
      query: (id) => ({ url: `/admin/package-types/${id}`, method: 'DELETE' }),
      invalidatesTags: ['PackageType'],
    }),

    // ── Departments ───────────────────────────────────────────────────────────
    listDepartments: build.query<Department[], void>({
      query: () => ({ url: '/admin/departments' }),
      providesTags: ['Department'],
    }),
    listActiveDepartments: build.query<Department[], void>({
      query: () => ({ url: '/admin/departments/active' }),
      providesTags: ['Department'],
    }),
    createDepartment: build.mutation<Department, { name: string; active: boolean }>({
      query: (data) => ({ url: '/admin/departments', method: 'POST', data }),
      invalidatesTags: ['Department'],
    }),
    updateDepartment: build.mutation<Department, { id: number; name: string; active: boolean }>({
      query: ({ id, ...data }) => ({ url: `/admin/departments/${id}`, method: 'PUT', data }),
      invalidatesTags: ['Department'],
    }),
    deleteDepartment: build.mutation<void, number>({
      query: (id) => ({ url: `/admin/departments/${id}`, method: 'DELETE' }),
      invalidatesTags: ['Department'],
    }),

    // ── Roles ─────────────────────────────────────────────────────────────────
    listRoles: build.query<PageResponse<Role>, { page?: number; size?: number }>({
      query: (params) => ({ url: '/admin/roles', params }),
      providesTags: ['Role'],
    }),
    listPermissions: build.query<Permission[], void>({
      query: () => ({ url: '/admin/permissions' }),
    }),
    createRole: build.mutation<Role, Partial<Role> & { permissionIds: number[] }>({
      query: (data) => ({ url: '/admin/roles', method: 'POST', data }),
      invalidatesTags: ['Role'],
    }),
    updateRole: build.mutation<Role, { id: number } & Partial<Role> & { permissionIds: number[] }>({
      query: ({ id, ...data }) => ({ url: `/admin/roles/${id}`, method: 'PUT', data }),
      invalidatesTags: ['Role'],
    }),
    deleteRole: build.mutation<void, number>({
      query: (id) => ({ url: `/admin/roles/${id}`, method: 'DELETE' }),
      invalidatesTags: ['Role'],
    }),

    // ── Users ─────────────────────────────────────────────────────────────────
    listUsers: build.query<PageResponse<UserAccount>, { search?: string; page?: number; size?: number }>({
      query: (params) => ({ url: '/admin/users', params: { ...params, search: params.search || undefined } }),
      providesTags: ['User'],
    }),
    createUser: build.mutation<UserAccount, Record<string, unknown>>({
      query: (data) => ({ url: '/admin/users', method: 'POST', data }),
      invalidatesTags: ['User'],
    }),
    updateUser: build.mutation<UserAccount, { id: number } & Record<string, unknown>>({
      query: ({ id, ...data }) => ({ url: `/admin/users/${id}`, method: 'PUT', data }),
      invalidatesTags: ['User'],
    }),
    deleteUser: build.mutation<void, number>({
      query: (id) => ({ url: `/admin/users/${id}`, method: 'DELETE' }),
      invalidatesTags: ['User'],
    }),

    // ── Approval Routing ──────────────────────────────────────────────────────
    listApprovalRouting: build.query<ApprovalRouting[], void>({
      query: () => ({ url: '/admin/approval-routing' }),
      providesTags: ['ApprovalRouting'],
    }),
    createApprovalRouting: build.mutation<ApprovalRouting, Record<string, unknown>>({
      query: (data) => ({ url: '/admin/approval-routing', method: 'POST', data }),
      invalidatesTags: ['ApprovalRouting'],
    }),
    deleteApprovalRouting: build.mutation<void, number>({
      query: (id) => ({ url: `/admin/approval-routing/${id}`, method: 'DELETE' }),
      invalidatesTags: ['ApprovalRouting'],
    }),

    // ── Company Settings ──────────────────────────────────────────────────────
    getCompanySettings: build.query<CompanySettings, void>({
      query: () => ({ url: '/admin/company-settings' }),
      providesTags: ['CompanySettings'],
    }),
    updateCompanySettings: build.mutation<CompanySettings, Record<string, unknown>>({
      query: (data) => ({ url: '/admin/company-settings', method: 'PUT', data }),
      invalidatesTags: ['CompanySettings'],
    }),

    // ── Audit Logs ────────────────────────────────────────────────────────────
    searchAuditLogs: build.query<PageResponse<AuditLog>, Record<string, unknown>>({
      query: (params) => ({ url: '/admin/audit-logs', params }),
    }),

    // ── MFA Management ────────────────────────────────────────────────────────
    listUserMfaStatus: build.query<PageResponse<UserMfaStatus>, { search?: string; page?: number; size?: number }>({
      query: (params) => ({ url: '/admin/users/mfa-status', params }),
      providesTags: ['UserMfa'],
    }),
    adminDisableMfa: build.mutation<void, number>({
      query: (id) => ({ url: `/admin/users/${id}/mfa/disable`, method: 'POST' }),
      invalidatesTags: ['UserMfa'],
    }),
    adminResetMfa: build.mutation<void, number>({
      query: (id) => ({ url: `/admin/users/${id}/mfa/reset`, method: 'POST' }),
      invalidatesTags: ['UserMfa'],
    }),
  }),
});

export const {
  useListCourierWaysQuery, useListActiveCourierWaysQuery,
  useCreateCourierWayMutation, useUpdateCourierWayMutation, useDeleteCourierWayMutation,
  useListPackageTypesQuery, useListActivePackageTypesQuery,
  useCreatePackageTypeMutation, useUpdatePackageTypeMutation, useDeletePackageTypeMutation,
  useListDepartmentsQuery, useListActiveDepartmentsQuery,
  useCreateDepartmentMutation, useUpdateDepartmentMutation, useDeleteDepartmentMutation,
  useListRolesQuery, useListPermissionsQuery,
  useCreateRoleMutation, useUpdateRoleMutation, useDeleteRoleMutation,
  useListUsersQuery, useCreateUserMutation, useUpdateUserMutation, useDeleteUserMutation,
  useListApprovalRoutingQuery, useCreateApprovalRoutingMutation, useDeleteApprovalRoutingMutation,
  useGetCompanySettingsQuery, useUpdateCompanySettingsMutation,
  useSearchAuditLogsQuery,
  useListUserMfaStatusQuery, useAdminDisableMfaMutation, useAdminResetMfaMutation,
} = adminApiSlice;
