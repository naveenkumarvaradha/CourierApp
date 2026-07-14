package com.courierapp.auth.controller;

import com.courierapp.auth.dto.admin.CompanyResponse;
import com.courierapp.auth.dto.auth.*;
import com.courierapp.auth.entity.Company;
import com.courierapp.auth.exception.BusinessException;
import com.courierapp.auth.repository.CompanyRepository;
import com.courierapp.auth.security.CookieUtil;
import com.courierapp.auth.security.JwtService;
import com.courierapp.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static com.courierapp.auth.security.CookieUtil.*;

/**
 * Tokens are never returned in the JSON body — they're set as httpOnly, Secure cookies so
 * client-side JS (and therefore any XSS payload) can never read them. Cookies are scoped
 * to /api/auth where possible to limit what's sent on ordinary API calls.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CompanyRepository companyRepository;
    private final CookieUtil cookieUtil;
    private final JwtService jwtService;

    @GetMapping("/companies")
    public ResponseEntity<List<CompanyResponse>> listCompanies() {
        List<CompanyResponse> list = companyRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(c -> new CompanyResponse(c.getId(), c.getCompanyCode(), c.getName())).toList();
        return ResponseEntity.ok(list);
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse tokens = authService.login(request);
        if (tokens.mfaRequired()) {
            ResponseCookie mfaCookie = cookieUtil.build(MFA_PENDING_COOKIE, tokens.mfaPendingToken(),
                    jwtService.getMfaPendingExpirySeconds(), "/api/auth", "Strict");
            return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, mfaCookie.toString())
                    .body(new TokenResponse(null, null, null, 0, true, null));
        }
        return withAuthCookies(tokens);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @CookieValue(value = REFRESH_COOKIE, required = false) String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException("Missing refresh token", HttpStatus.UNAUTHORIZED);
        }
        TokenResponse tokens = authService.refresh(new RefreshRequest(refreshToken));
        return withAuthCookies(tokens);
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
            @CookieValue(value = ACCESS_COOKIE, required = false) String accessToken,
            @CookieValue(value = REFRESH_COOKIE, required = false) String refreshToken) {
        if (accessToken != null && !accessToken.isBlank()) {
            authService.logout(accessToken, refreshToken);
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieUtil.clear(ACCESS_COOKIE, "/api").toString())
                .header(HttpHeaders.SET_COOKIE, cookieUtil.clear(REFRESH_COOKIE, "/api/auth").toString())
                .header(HttpHeaders.SET_COOKIE, cookieUtil.clear(MFA_PENDING_COOKIE, "/api/auth").toString())
                .body(Map.of("message", "Logged out successfully"));
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
    public ResponseEntity<TokenResponse> confirmMfa(
            @CookieValue(value = MFA_PENDING_COOKIE, required = false) String mfaPendingToken,
            @Valid @RequestBody MfaVerifyRequest request) {
        if (mfaPendingToken == null || mfaPendingToken.isBlank()) {
            throw new BusinessException("MFA session expired — please log in again", HttpStatus.UNAUTHORIZED);
        }
        TokenResponse tokens = authService.confirmMfa(new MfaConfirmRequest(mfaPendingToken, request.code()));
        return withAuthCookiesAndClearMfa(tokens);
    }

    private ResponseEntity<TokenResponse> withAuthCookies(TokenResponse tokens) {
        ResponseCookie accessCookie = cookieUtil.build(ACCESS_COOKIE, tokens.accessToken(),
                jwtService.getAccessExpirySeconds(), "/api", "Lax");
        ResponseCookie refreshCookie = cookieUtil.build(REFRESH_COOKIE, tokens.refreshToken(),
                jwtService.getRefreshExpirySeconds(), "/api/auth", "Strict");
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(new TokenResponse(null, null, "Bearer", tokens.expiresIn(), false, null));
    }

    private ResponseEntity<TokenResponse> withAuthCookiesAndClearMfa(TokenResponse tokens) {
        ResponseCookie accessCookie = cookieUtil.build(ACCESS_COOKIE, tokens.accessToken(),
                jwtService.getAccessExpirySeconds(), "/api", "Lax");
        ResponseCookie refreshCookie = cookieUtil.build(REFRESH_COOKIE, tokens.refreshToken(),
                jwtService.getRefreshExpirySeconds(), "/api/auth", "Strict");
        ResponseCookie clearedMfaCookie = cookieUtil.clear(MFA_PENDING_COOKIE, "/api/auth");
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .header(HttpHeaders.SET_COOKIE, clearedMfaCookie.toString())
                .body(new TokenResponse(null, null, "Bearer", tokens.expiresIn(), false, null));
    }
}
