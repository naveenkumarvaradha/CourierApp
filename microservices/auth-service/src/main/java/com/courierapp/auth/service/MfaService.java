package com.courierapp.auth.service;

public interface MfaService {
    record MfaSetupResult(String secret, String qrDataUri) {}
    MfaSetupResult generateSecret(String username);
    boolean verifyCode(String secret, String code);
}
