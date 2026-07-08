package com.courierapp.service;

public interface MfaService {
    /** Generate a new TOTP secret for a user and return (secret, otpauth URI). */
    MfaSetupResult generateSecret(String username);

    /** Verify a 6-digit OTP against the user's stored secret. */
    boolean verifyCode(String secret, String code);

    record MfaSetupResult(String secret, String qrDataUri) {}
}
