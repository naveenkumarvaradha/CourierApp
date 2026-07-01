package com.courierapp.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompanyRequest(
        @NotBlank @Size(max = 20) String companyCode,
        @NotBlank @Size(max = 255) String name,
        boolean active
) {}
