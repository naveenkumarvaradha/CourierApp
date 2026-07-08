package com.courierapp.booking.dto.booking;

import jakarta.validation.constraints.NotBlank;

public record AwbUpdateRequest(@NotBlank String awbNumber) {}
