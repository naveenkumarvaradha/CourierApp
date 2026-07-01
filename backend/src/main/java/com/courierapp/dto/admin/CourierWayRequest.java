package com.courierapp.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CourierWayRequest(
        @NotBlank @Size(max = 100) String name,
        boolean active
) {}
