package com.courierapp.dto.auth;

public record MfaSetupResponse(String qrDataUri, String secret) {}
