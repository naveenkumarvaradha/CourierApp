package com.courierapp.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompanyRequest(
        @Size(max = 20) String companyCode,   // optional — auto-generated on create
        @NotBlank @Size(max = 255) String name,
        boolean active
) {}
