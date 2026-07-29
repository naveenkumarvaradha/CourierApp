package com.courierapp.dto.dc;

import com.courierapp.dto.admin.UnitResponse;
import com.courierapp.dto.booking.BookingResponse;
import com.courierapp.enums.DcStatus;

import java.time.Instant;
import java.time.LocalDate;

public record DcResponse(
        Long id,
        String dcNumber,
        LocalDate dcDate,
        BookingResponse booking,
        UnitResponse unit,
        String vehicleNumber,
        String driverName,
        DcStatus status,
        String remarks,
        Instant createdAt,
        String createdBy,
        Instant updatedAt,
        String updatedBy
) {
}
