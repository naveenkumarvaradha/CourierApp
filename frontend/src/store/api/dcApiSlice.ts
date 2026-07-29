import { baseApi } from './baseApi';
import type { DeliveryChallan, PageResponse } from '../../types';

type DcFilters = Record<string, string | number | boolean | undefined | null>;

export const dcApiSlice = baseApi.injectEndpoints({
  endpoints: (build) => ({
    searchDcs: build.query<PageResponse<DeliveryChallan>, DcFilters>({
      query: (params) => ({ url: '/dc', params }),
      providesTags: (result) =>
        result
          ? [...result.content.map(({ id }) => ({ type: 'DeliveryChallan' as const, id })), 'DeliveryChallan']
          : ['DeliveryChallan'],
    }),
    getDc: build.query<DeliveryChallan, number>({
      query: (id) => ({ url: `/dc/${id}` }),
      providesTags: (_r, _e, id) => [{ type: 'DeliveryChallan', id }],
    }),
    getDcByBooking: build.query<DeliveryChallan | null, number>({
      query: (bookingId) => ({ url: `/dc/by-booking/${bookingId}` }),
      transformResponse: (response: DeliveryChallan | '' | null | undefined) => response || null,
      providesTags: ['DeliveryChallan'],
    }),
    createDc: build.mutation<DeliveryChallan, Record<string, unknown>>({
      query: (data) => ({ url: '/dc', method: 'POST', data }),
      invalidatesTags: ['DeliveryChallan'],
    }),
    updateDc: build.mutation<DeliveryChallan, { id: number; data: Record<string, unknown> }>({
      query: ({ id, data }) => ({ url: `/dc/${id}`, method: 'PUT', data }),
      invalidatesTags: (_r, _e, { id }) => [{ type: 'DeliveryChallan', id }, 'DeliveryChallan'],
    }),
    deleteDc: build.mutation<void, number>({
      query: (id) => ({ url: `/dc/${id}`, method: 'DELETE' }),
      invalidatesTags: ['DeliveryChallan'],
    }),
    changeDcStatus: build.mutation<DeliveryChallan, { id: number; status: string }>({
      query: ({ id, status }) => ({ url: `/dc/${id}/status`, method: 'POST', data: { status } }),
      invalidatesTags: (_r, _e, { id }) => [{ type: 'DeliveryChallan', id }, 'DeliveryChallan'],
    }),
  }),
});

export const {
  useSearchDcsQuery,
  useGetDcQuery,
  useGetDcByBookingQuery,
  useCreateDcMutation,
  useUpdateDcMutation,
  useDeleteDcMutation,
  useChangeDcStatusMutation,
} = dcApiSlice;
