package com.courierapp.service;

import com.courierapp.dto.auth.*;

public interface AuthService {
    TokenResponse login(LoginRequest request);
    TokenResponse refresh(RefreshRequest request);
    CurrentUserResponse currentUser(String username);
    void changePassword(String username, ChangePasswordRequest request);
    void forgotPassword(ForgotPasswordRequest request);
    void resetPassword(ResetPasswordRequest request);
    void logout(String token);
    MfaSetupResponse setupMfa(String username);
    void enableMfa(String username, String code);
    void disableMfa(String username);
    TokenResponse confirmMfa(MfaConfirmRequest request);
}
