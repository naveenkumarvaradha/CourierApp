package com.courierapp.auth.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String companyCode,
        @NotBlank String username,
        @NotBlank String password) {}
