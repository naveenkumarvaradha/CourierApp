package com.courierapp.admin.dto.admin;

import jakarta.validation.constraints.NotBlank;

public record DepartmentRequest(@NotBlank String name, boolean active) {}
