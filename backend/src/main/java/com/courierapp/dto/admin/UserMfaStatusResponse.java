package com.courierapp.dto.admin;

public record UserMfaStatusResponse(
        Long id,
        String username,
        String fullName,
        String email,
        boolean mfaEnabled,
        boolean mfaConfigured
) {}
