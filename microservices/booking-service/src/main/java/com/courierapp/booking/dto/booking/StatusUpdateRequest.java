package com.courierapp.booking.dto.booking;

import com.courierapp.booking.enums.BookingStatus;
import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(@NotNull BookingStatus status, String remarks) {}
