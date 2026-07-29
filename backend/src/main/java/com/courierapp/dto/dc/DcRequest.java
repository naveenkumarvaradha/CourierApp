package com.courierapp.dto.dc;

import com.courierapp.enums.DcStatus;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record DcRequest(
        @NotNull(message = "Booking is required")
        Long bookingId,

        @NotNull(message = "Unit is required")
        Long unitId,

        @NotNull(message = "DC date is required")
        LocalDate dcDate,

        @Size(max = 30)
        String vehicleNumber,

        @Size(max = 100)
        String driverName,

        @NotNull(message = "Status is required")
        DcStatus status,

        @Size(max = 500)
        String remarks
) {
}
