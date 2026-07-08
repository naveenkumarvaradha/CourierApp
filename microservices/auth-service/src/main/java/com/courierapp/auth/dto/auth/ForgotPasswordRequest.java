package com.courierapp.auth.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(@NotBlank String username) {}
