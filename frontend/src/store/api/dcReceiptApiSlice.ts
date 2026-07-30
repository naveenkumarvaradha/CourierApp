import { baseApi } from './baseApi';
import type { DcReceipt, DeliveryChallan, PageResponse } from '../../types';

type Filters = Record<string, string | number | boolean | undefined | null>;

export const dcReceiptApiSlice = baseApi.injectEndpoints({
  endpoints: (build) => ({
    searchDcReceipts: build.query<PageResponse<DcReceipt>, Filters>({
      query: (params) => ({ url: '/dc-receipts', params }),
      providesTags: (result) =>
        result
          ? [...result.content.map(({ id }) => ({ type: 'DcReceipt' as const, id })), 'DcReceipt']
          : ['DcReceipt'],
    }),
    getDcReceipt: build.query<DcReceipt, number>({
      query: (id) => ({ url: `/dc-receipts/${id}` }),
      providesTags: (_r, _e, id) => [{ type: 'DcReceipt', id }],
    }),
    listEligibleDcsForReceipt: build.query<PageResponse<DeliveryChallan>, Filters | void>({
      query: (params) => ({ url: '/dc-receipts/eligible', params: params ?? { size: 200 } }),
      providesTags: ['DeliveryChallan'],
    }),
    createDcReceipt: build.mutation<DcReceipt, Record<string, unknown>>({
      query: (data) => ({ url: '/dc-receipts', method: 'POST', data }),
      invalidatesTags: ['DcReceipt', 'DeliveryChallan'],
    }),
    deleteDcReceipt: build.mutation<void, number>({
      query: (id) => ({ url: `/dc-receipts/${id}`, method: 'DELETE' }),
      invalidatesTags: ['DcReceipt', 'DeliveryChallan'],
    }),
  }),
});

export const {
  useSearchDcReceiptsQuery,
  useGetDcReceiptQuery,
  useListEligibleDcsForReceiptQuery,
  useCreateDcReceiptMutation,
  useDeleteDcReceiptMutation,
} = dcReceiptApiSlice;
