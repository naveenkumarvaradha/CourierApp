package com.courierapp.admin.dto.admin;

import jakarta.validation.constraints.NotBlank;

public record CompanyRequest(@NotBlank String name, String companyCode, boolean active) {}
