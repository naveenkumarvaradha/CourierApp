package com.courierapp.auth.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MfaConfirmRequest(
        @NotBlank String mfaPendingToken,
        @NotBlank @Pattern(regexp = "\\d{6}") String code) {}
