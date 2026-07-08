package com.courierapp.admin.dto.admin;

import jakarta.validation.constraints.NotBlank;

public record CourierWayRequest(@NotBlank String name, boolean active) {}
