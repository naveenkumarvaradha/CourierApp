import { createApi } from '@reduxjs/toolkit/query/react';
import type { BaseQueryFn } from '@reduxjs/toolkit/query';
import { api } from '../../api/client';
import type { AxiosRequestConfig } from 'axios';

type AxiosArgs = { url: string; method?: AxiosRequestConfig['method']; data?: unknown; params?: unknown };

const axiosBaseQuery: BaseQueryFn<AxiosArgs, unknown, { status?: number; data?: unknown }> =
  async ({ url, method = 'GET', data, params }) => {
    try {
      const result = await api({ url, method, data, params });
      return { data: result.data };
    } catch (err: any) {
      return {
        error: {
          status: err?.response?.status,
          data: err?.response?.data ?? err?.message,
        },
      };
    }
  };

export const baseApi = createApi({
  reducerPath: 'api',
  baseQuery: axiosBaseQuery,
  tagTypes: ['DashboardTasks', 'ReportSummary', 'Booking', 'Party', 'CourierWay', 'PackageType', 'Department', 'User', 'Role', 'ApprovalRouting', 'CompanySettings', 'UserMfa'],
  endpoints: () => ({}),
});
