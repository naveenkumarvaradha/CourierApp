package com.courierapp.booking.dto.dashboard;

import com.courierapp.booking.dto.booking.BookingResponse;

import java.util.List;

public record DashboardResponse(
        List<BookingResponse> bookingsPendingMyApproval,
        List<BookingResponse> myBookingsPendingSent,
        List<BookingResponse> pendingToPrint,
        List<BookingResponse> allPendingApprovalBookings
) {}
