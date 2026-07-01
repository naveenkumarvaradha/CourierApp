package com.courierapp.dto.report;

import com.courierapp.dto.booking.BookingResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record ReportSummaryResponse(
        LocalDate fromDate,
        LocalDate toDate,
        String granularity,
        long totalBookings,
        BigDecimal totalCharges,
        BigDecimal totalFreight,
        BigDecimal totalDeclaredValue,
        Map<String, Long> countByStatus,
        Map<String, Long> countByMode,
        Map<String, BigDecimal> chargesByMode,
        List<PartyBreakdown> bySender,
        List<PartyBreakdown> byReceiver,
        List<BookingResponse> bookings
) {
    public record PartyBreakdown(
            String partyCode,
            String partyName,
            long bookingCount,
            BigDecimal totalCharges
    ) {
    }
}
