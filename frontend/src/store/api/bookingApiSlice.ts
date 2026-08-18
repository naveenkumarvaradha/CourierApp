import { baseApi } from './baseApi';
import type { Booking, PageResponse } from '../../types';

type BookingFilters = Record<string, string | number | boolean | undefined | null>;

export interface TrackingEvent {
  provider: string;
  status: string;
  description: string | null;
  location: string | null;
  eventTime: string | null;
}

export const bookingApiSlice = baseApi.injectEndpoints({
  endpoints: (build) => ({
    searchBookings: build.query<PageResponse<Booking>, BookingFilters>({
      query: (params) => ({ url: '/bookings', params }),
      providesTags: (result) =>
        result
          ? [...result.content.map(({ id }) => ({ type: 'Booking' as const, id })), 'Booking']
          : ['Booking'],
    }),
    getBooking: build.query<Booking, number>({
      query: (id) => ({ url: `/bookings/${id}` }),
      providesTags: (_r, _e, id) => [{ type: 'Booking', id }],
    }),
    createBooking: build.mutation<Booking, Record<string, unknown>>({
      query: (data) => ({ url: '/bookings', method: 'POST', data }),
      invalidatesTags: ['Booking', 'DashboardTasks'],
    }),
    updateBooking: build.mutation<Booking, { id: number; data: Record<string, unknown> }>({
      query: ({ id, data }) => ({ url: `/bookings/${id}`, method: 'PUT', data }),
      invalidatesTags: (_r, _e, { id }) => [{ type: 'Booking', id }, 'DashboardTasks'],
    }),
    deleteBooking: build.mutation<void, number>({
      query: (id) => ({ url: `/bookings/${id}`, method: 'DELETE' }),
      invalidatesTags: ['Booking', 'DashboardTasks'],
    }),
    submitBooking: build.mutation<Booking, number>({
      query: (id) => ({ url: `/bookings/${id}/submit`, method: 'POST' }),
      invalidatesTags: (_r, _e, id) => [{ type: 'Booking', id }, 'Booking', 'DashboardTasks'],
    }),
    approveBooking: build.mutation<Booking, { id: number; remarks: string }>({
      query: ({ id, remarks }) => ({ url: `/bookings/${id}/approve`, method: 'POST', data: { remarks } }),
      invalidatesTags: (_r, _e, { id }) => [{ type: 'Booking', id }, 'Booking', 'DashboardTasks', 'ReportSummary'],
    }),
    rejectBooking: build.mutation<Booking, { id: number; remarks: string }>({
      query: ({ id, remarks }) => ({ url: `/bookings/${id}/reject`, method: 'POST', data: { remarks } }),
      invalidatesTags: (_r, _e, { id }) => [{ type: 'Booking', id }, 'Booking', 'DashboardTasks'],
    }),
    changeBookingStatus: build.mutation<Booking, { id: number; status: string }>({
      query: ({ id, status }) => ({ url: `/bookings/${id}/status`, method: 'POST', data: { status } }),
      invalidatesTags: (_r, _e, { id }) => [{ type: 'Booking', id }, 'Booking', 'DashboardTasks'],
    }),
    updateAwb: build.mutation<Booking, { id: number; awbNumber: string }>({
      query: ({ id, awbNumber }) => ({ url: `/bookings/${id}/awb`, method: 'PUT', data: { awbNumber } }),
      invalidatesTags: (_r, _e, { id }) => [{ type: 'Booking', id }, 'Booking', 'DashboardTasks'],
    }),
    reviseBooking: build.mutation<Booking, number>({
      query: (id) => ({ url: `/bookings/${id}/revise`, method: 'POST' }),
      invalidatesTags: (_r, _e, id) => [{ type: 'Booking', id }, 'Booking'],
    }),
    requestCancellation: build.mutation<Booking, { id: number; remarks?: string }>({
      query: ({ id, remarks }) => ({ url: `/bookings/${id}/request-cancellation`, method: 'POST', params: remarks ? { remarks } : undefined }),
      invalidatesTags: (_r, _e, { id }) => [{ type: 'Booking', id }, 'Booking', 'DashboardTasks'],
    }),
    approveCancellation: build.mutation<Booking, number>({
      query: (id) => ({ url: `/bookings/${id}/approve-cancellation`, method: 'POST' }),
      invalidatesTags: (_r, _e, id) => [{ type: 'Booking', id }, 'Booking', 'DashboardTasks'],
    }),
    rejectCancellation: build.mutation<Booking, number>({
      query: (id) => ({ url: `/bookings/${id}/reject-cancellation`, method: 'POST' }),
      invalidatesTags: (_r, _e, id) => [{ type: 'Booking', id }, 'Booking', 'DashboardTasks'],
    }),
    getTracking: build.query<TrackingEvent[], number>({
      query: (id) => ({ url: `/bookings/${id}/tracking` }),
      providesTags: (_r, _e, id) => [{ type: 'Tracking' as const, id }],
    }),
  }),
});

export const {
  useSearchBookingsQuery,
  useGetBookingQuery,
  useCreateBookingMutation,
  useUpdateBookingMutation,
  useDeleteBookingMutation,
  useSubmitBookingMutation,
  useApproveBookingMutation,
  useRejectBookingMutation,
  useChangeBookingStatusMutation,
  useUpdateAwbMutation,
  useReviseBookingMutation,
  useRequestCancellationMutation,
  useApproveCancellationMutation,
  useRejectCancellationMutation,
  useLazyGetTrackingQuery,
} = bookingApiSlice;
