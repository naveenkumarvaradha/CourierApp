package com.courierapp.controller;

import com.courierapp.dto.admin.CompanyResponse;
import com.courierapp.dto.auth.*;
import com.courierapp.service.AdminService;
import com.courierapp.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;
    private final AdminService adminService;

    public AuthController(AuthService authService, AdminService adminService) {
        this.authService = authService;
        this.adminService = adminService;
    }

    @GetMapping("/companies")
    @Operation(summary = "List active companies for login page")
    public ResponseEntity<List<CompanyResponse>> listCompanies() {
        return ResponseEntity.ok(adminService.listActiveCompanies());
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate and obtain access & refresh tokens")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Exchange a refresh token for a new token pair")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Get the current authenticated user with roles & permissions")
    public ResponseEntity<CurrentUserResponse> me(Authentication authentication) {
        return ResponseEntity.ok(authService.currentUser(authentication.getName()));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change password for the currently logged-in user")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication) {
        authService.changePassword(authentication.getName(), request);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request a password reset email")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(Map.of("message",
                "If an account with that email exists, a reset link has been sent."));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using a token from the email link")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", "Password reset successfully. You can now log in."));
    }

    @PostMapping("/logout")
    @Operation(summary = "Invalidate the current access token (blacklist it in Redis)")
    public ResponseEntity<Map<String, String>> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            authService.logout(authHeader.substring(7));
        }
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    // ── MFA endpoints ─────────────────────────────────────────────────────────

    @PostMapping("/setup-mfa")
    @Operation(summary = "Generate TOTP secret and QR code for the current user")
    public ResponseEntity<MfaSetupResponse> setupMfa(Authentication authentication) {
        return ResponseEntity.ok(authService.setupMfa(authentication.getName()));
    }

    @PostMapping("/enable-mfa")
    @Operation(summary = "Confirm OTP from Authenticator app to activate MFA on this account")
    public ResponseEntity<Map<String, String>> enableMfa(
            @Valid @RequestBody MfaVerifyRequest request,
            Authentication authentication) {
        authService.enableMfa(authentication.getName(), request.code());
        return ResponseEntity.ok(Map.of("message", "MFA enabled successfully"));
    }

    @PostMapping("/disable-mfa")
    @Operation(summary = "Disable MFA for the current user")
    public ResponseEntity<Map<String, String>> disableMfa(Authentication authentication) {
        authService.disableMfa(authentication.getName());
        return ResponseEntity.ok(Map.of("message", "MFA disabled"));
    }

    @PostMapping("/confirm-mfa")
    @Operation(summary = "Complete login by submitting OTP (called when mfaRequired=true in login response)")
    public ResponseEntity<TokenResponse> confirmMfa(@Valid @RequestBody MfaConfirmRequest request) {
        return ResponseEntity.ok(authService.confirmMfa(request));
    }
}
