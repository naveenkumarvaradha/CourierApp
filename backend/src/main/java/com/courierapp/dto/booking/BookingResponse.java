package com.courierapp.dto.booking;

import com.courierapp.dto.master.PartyResponse;
import com.courierapp.enums.BookingStatus;
import com.courierapp.enums.CourierMode;
import com.courierapp.enums.PaymentMode;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record BookingResponse(
        Long id,
        String bookingNumber,
        LocalDate bookingDate,
        PartyResponse sender,
        PartyResponse receiver,
        String itemDescription,
        BigDecimal weightKg,
        Integer noOfPackages,
        CourierMode courierMode,
        BigDecimal declaredValue,
        BigDecimal freightCharges,
        BigDecimal totalCharges,
        PaymentMode paymentMode,
        String specialInstructions,
        BookingStatus status,
        String approverUsername,
        Instant approvalTimestamp,
        String approvalRemarks,
        Instant createdAt,
        String createdBy,
        Instant updatedAt,
        String updatedBy
) {
}
