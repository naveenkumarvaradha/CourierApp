package com.courierapp.auth.controller;

import com.courierapp.auth.dto.admin.CompanyResponse;
import com.courierapp.auth.dto.auth.*;
import com.courierapp.auth.entity.Company;
import com.courierapp.auth.repository.CompanyRepository;
import com.courierapp.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CompanyRepository companyRepository;

    @GetMapping("/companies")
    public ResponseEntity<List<CompanyResponse>> listCompanies() {
        List<CompanyResponse> list = companyRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(c -> new CompanyResponse(c.getId(), c.getCompanyCode(), c.getName())).toList();
        return ResponseEntity.ok(list);
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> me(Authentication authentication) {
        return ResponseEntity.ok(authService.currentUser(authentication.getName()));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request, Authentication authentication) {
        authService.changePassword(authentication.getName(), request);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(Map.of("message", "If an account with that email exists, a reset link has been sent."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", "Password reset successfully."));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody(required = false) LogoutRequest request) {
        if (authHeader != null && authHeader.startsWith("Bearer "))
            authService.logout(authHeader.substring(7), request != null ? request.refreshToken() : null);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @PostMapping("/setup-mfa")
    public ResponseEntity<MfaSetupResponse> setupMfa(Authentication authentication) {
        return ResponseEntity.ok(authService.setupMfa(authentication.getName()));
    }

    @PostMapping("/enable-mfa")
    public ResponseEntity<Map<String, String>> enableMfa(
            @Valid @RequestBody MfaVerifyRequest request, Authentication authentication) {
        authService.enableMfa(authentication.getName(), request.code());
        return ResponseEntity.ok(Map.of("message", "MFA enabled successfully"));
    }

    @PostMapping("/disable-mfa")
    public ResponseEntity<Map<String, String>> disableMfa(Authentication authentication) {
        authService.disableMfa(authentication.getName());
        return ResponseEntity.ok(Map.of("message", "MFA disabled"));
    }

    @PostMapping("/confirm-mfa")
    public ResponseEntity<TokenResponse> confirmMfa(@Valid @RequestBody MfaConfirmRequest request) {
        return ResponseEntity.ok(authService.confirmMfa(request));
    }
}
