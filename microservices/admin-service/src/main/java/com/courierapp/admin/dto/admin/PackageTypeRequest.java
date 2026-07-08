package com.courierapp.admin.dto.admin;

import jakarta.validation.constraints.NotBlank;

public record PackageTypeRequest(@NotBlank String name, boolean active) {}
