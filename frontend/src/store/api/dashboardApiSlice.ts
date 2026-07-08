import { baseApi } from './baseApi';
import type { DashboardTasks, ReportSummary } from '../../types';

export const dashboardApiSlice = baseApi.injectEndpoints({
  endpoints: (build) => ({
    getDashboardTasks: build.query<DashboardTasks, void>({
      query: () => ({ url: '/dashboard' }),
      providesTags: ['DashboardTasks'],
    }),
    getReportSummary: build.query<ReportSummary, { granularity: string; from?: string; to?: string }>({
      query: (params) => ({ url: '/reports/summary', params }),
      providesTags: ['ReportSummary'],
    }),
  }),
});

export const { useGetDashboardTasksQuery, useGetReportSummaryQuery } = dashboardApiSlice;
