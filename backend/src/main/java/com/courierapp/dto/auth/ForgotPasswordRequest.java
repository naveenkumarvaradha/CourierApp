package com.courierapp.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(@NotBlank String username) {}
