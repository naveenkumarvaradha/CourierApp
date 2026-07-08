package com.courierapp.auth.dto.auth;

public record MfaSetupResponse(String qrDataUri, String secret) {}
