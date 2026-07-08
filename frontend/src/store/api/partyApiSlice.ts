import { baseApi } from './baseApi';
import type { Party, PageResponse } from '../../types';

type PartyFilters = { name?: string; city?: string; pincode?: string; page?: number; size?: number };

export const partyApiSlice = baseApi.injectEndpoints({
  endpoints: (build) => ({
    searchParties: build.query<PageResponse<Party>, PartyFilters>({
      query: (params) => ({ url: '/master/parties', params }),
      providesTags: (result) =>
        result
          ? [...result.content.map(({ id }) => ({ type: 'Party' as const, id })), 'Party']
          : ['Party'],
    }),
    listActiveParties: build.query<Party[], void>({
      query: () => ({ url: '/master/parties/active' }),
      providesTags: ['Party'],
    }),
    getParty: build.query<Party, number>({
      query: (id) => ({ url: `/master/parties/${id}` }),
      providesTags: (_r, _e, id) => [{ type: 'Party', id }],
    }),
    createParty: build.mutation<Party, Record<string, unknown>>({
      query: (data) => ({ url: '/master/parties', method: 'POST', data }),
      invalidatesTags: ['Party', 'DashboardTasks'],
    }),
    updateParty: build.mutation<Party, { id: number; data: Record<string, unknown> }>({
      query: ({ id, data }) => ({ url: `/master/parties/${id}`, method: 'PUT', data }),
      invalidatesTags: (_r, _e, { id }) => [{ type: 'Party', id }, 'Party', 'DashboardTasks'],
    }),
    deleteParty: build.mutation<void, number>({
      query: (id) => ({ url: `/master/parties/${id}`, method: 'DELETE' }),
      invalidatesTags: ['Party', 'DashboardTasks'],
    }),
    approveParty: build.mutation<Party, number>({
      query: (id) => ({ url: `/master/parties/${id}/approve`, method: 'POST' }),
      invalidatesTags: (_r, _e, id) => [{ type: 'Party', id }, 'Party', 'DashboardTasks'],
    }),
    rejectParty: build.mutation<Party, { id: number; remarks?: string }>({
      query: ({ id, remarks }) => ({ url: `/master/parties/${id}/reject`, method: 'POST', params: remarks ? { remarks } : undefined }),
      invalidatesTags: (_r, _e, { id }) => [{ type: 'Party', id }, 'Party', 'DashboardTasks'],
    }),
  }),
});

export const {
  useSearchPartiesQuery,
  useListActivePartiesQuery,
  useGetPartyQuery,
  useCreatePartyMutation,
  useUpdatePartyMutation,
  useDeletePartyMutation,
  useApprovePartyMutation,
  useRejectPartyMutation,
} = partyApiSlice;
