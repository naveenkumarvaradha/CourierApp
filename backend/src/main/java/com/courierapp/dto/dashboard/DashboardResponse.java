package com.courierapp.dto.dashboard;

import com.courierapp.dto.booking.BookingResponse;
import com.courierapp.dto.master.PartyResponse;

import java.util.List;

public record DashboardResponse(
        // Bookings in PENDING_APPROVAL that this user can approve
        List<BookingResponse> bookingsPendingMyApproval,

        // Bookings I created that are still PENDING_APPROVAL
        List<BookingResponse> myBookingsPendingSent,

        // Parties in PENDING_APPROVAL that this user can approve
        List<PartyResponse> partiesPendingMyApproval,

        // Parties I created that are still PENDING_APPROVAL
        List<PartyResponse> myPartiesPendingSent,

        // APPROVED bookings in this company that have not yet been printed
        List<BookingResponse> pendingToPrint,

        // All PENDING_APPROVAL bookings in this company (visible to BOOKING_VIEW users)
        // Each entry carries pendingApprovers so viewer knows who needs to act
        List<BookingResponse> allPendingApprovalBookings,

        // All PENDING_APPROVAL parties in this company (visible to MASTER_VIEW users)
        List<PartyResponse> allPendingApprovalParties
) {}
