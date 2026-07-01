package com.courierapp.dto.booking;

import com.courierapp.enums.BookingStatus;
import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(
        @NotNull(message = "Target status is required")
        BookingStatus status
) {
}
