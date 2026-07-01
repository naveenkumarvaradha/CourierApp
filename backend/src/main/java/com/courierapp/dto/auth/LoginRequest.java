package com.courierapp.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Company code is required") String companyCode,
        @NotBlank(message = "Username is required") String username,
        @NotBlank(message = "Password is required") String password
) {}
