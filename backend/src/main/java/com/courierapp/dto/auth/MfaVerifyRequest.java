package com.courierapp.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MfaVerifyRequest(
        @NotBlank @Pattern(regexp = "\\d{6}", message = "OTP must be 6 digits") String code
) {}
