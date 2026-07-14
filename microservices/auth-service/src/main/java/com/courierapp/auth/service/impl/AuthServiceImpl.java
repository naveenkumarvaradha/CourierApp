package com.courierapp.auth.service.impl;

import com.courierapp.auth.dto.auth.*;
import com.courierapp.auth.entity.Company;
import com.courierapp.auth.entity.PasswordResetToken;
import com.courierapp.auth.entity.User;
import com.courierapp.auth.exception.BusinessException;
import com.courierapp.auth.exception.ResourceNotFoundException;
import com.courierapp.auth.repository.CompanyRepository;
import com.courierapp.auth.repository.PasswordResetTokenRepository;
import com.courierapp.auth.repository.UserRepository;
import com.courierapp.auth.security.*;
import com.courierapp.auth.service.*;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final EmailService emailService;
    private final TokenBlacklistService tokenBlacklistService;
    private final MfaService mfaService;
    private final SessionTrackingService sessionTrackingService;
    private final LoginAttemptService loginAttemptService;

    @Override
    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        String attemptKey = (request.companyCode() + ":" + request.username()).toLowerCase();
        if (loginAttemptService.isLocked(attemptKey)) {
            auditLogService.log("AUTH", "LOGIN_LOCKED", null, request.username(), request.username(),
                    "Too many failed attempts — temporarily locked");
            throw new BusinessException("Too many failed login attempts. Please try again in a few minutes.",
                    HttpStatus.TOO_MANY_REQUESTS);
        }
        try {
            Company company = companyRepository.findByCompanyCodeIgnoreCase(request.companyCode())
                    .orElseThrow(() -> new BadCredentialsException("Invalid company code or credentials"));
            if (!company.isActive()) throw new BadCredentialsException("Company account is inactive");
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
            AppUserPrincipal principal = (AppUserPrincipal) auth.getPrincipal();
            User user = userRepository.findById(principal.getId())
                    .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
            if (user.getCompany() == null || !user.getCompany().getId().equals(company.getId())) {
                auditLogService.log("AUTH", "LOGIN_FAILED", principal.getId(), request.username(),
                        request.username(), "Wrong company: " + request.companyCode());
                throw new BadCredentialsException("Invalid company code or credentials");
            }
            if (user.isMfaEnabled() && user.getMfaSecret() != null) {
                String pending = jwtService.generateMfaPendingToken(principal.getUsername(), user.getId());
                loginAttemptService.recordSuccess(attemptKey);
                return TokenResponse.mfaRequired(pending);
            }
            List<String> authorities = principal.getAuthorityStrings().stream().sorted().toList();
            String access = jwtService.generateAccessToken(principal.getUsername(), principal.getId(), company.getId(), authorities);
            String refresh = jwtService.generateRefreshToken(principal.getUsername(), principal.getId());
            auditLogService.log("AUTH", "LOGIN", principal.getId(), principal.getUsername(),
                    principal.getUsername(), "Company: " + company.getCompanyCode());
            TokenResponse response = TokenResponse.bearer(access, refresh, jwtService.getAccessExpirySeconds());
            sessionTrackingService.registerSession(principal.getId(), principal.getUsername(), access);
            loginAttemptService.recordSuccess(attemptKey);
            return response;
        } catch (BadCredentialsException ex) {
            loginAttemptService.recordFailure(attemptKey);
            auditLogService.log("AUTH", "LOGIN_FAILED", null, request.username(), request.username(), "Invalid credentials");
            throw ex;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TokenResponse refresh(RefreshRequest request) {
        final Claims claims;
        try {
            claims = jwtService.parse(request.refreshToken());
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BusinessException("Invalid or expired refresh token", HttpStatus.UNAUTHORIZED);
        }
        if (!jwtService.isRefreshToken(claims))
            throw new BusinessException("Provided token is not a refresh token", HttpStatus.UNAUTHORIZED);
        if (tokenBlacklistService.isBlacklisted(request.refreshToken()))
            throw new BusinessException("Refresh token has already been used", HttpStatus.UNAUTHORIZED);
        String username = claims.getSubject();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("User no longer exists", HttpStatus.UNAUTHORIZED));
        if (!user.isActive()) throw new BusinessException("User account is disabled", HttpStatus.UNAUTHORIZED);
        AppUserPrincipal principal = new AppUserPrincipal(user);
        List<String> authorities = principal.getAuthorityStrings().stream().sorted().toList();
        Long companyId = user.getCompany() != null ? user.getCompany().getId() : null;
        String access = jwtService.generateAccessToken(username, user.getId(), companyId, authorities);
        String newRefresh = jwtService.generateRefreshToken(username, user.getId());
        // Rotation: the presented refresh token is single-use — blacklist it immediately so a
        // captured/replayed copy can never be exchanged again.
        tokenBlacklistService.blacklist(request.refreshToken(), claims.getExpiration());
        return TokenResponse.bearer(access, newRefresh, jwtService.getAccessExpirySeconds());
    }

    @Override
    @Transactional(readOnly = true)
    public CurrentUserResponse currentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));
        List<String> roles = user.getRoles().stream().map(r -> r.getName()).sorted().toList();
        Set<String> permissions = new AppUserPrincipal(user).getAuthorityStrings().stream()
                .filter(a -> !a.startsWith("ROLE_")).collect(Collectors.toCollection(java.util.TreeSet::new));
        Company company = user.getCompany();
        return new CurrentUserResponse(user.getId(), user.getUsername(), user.getFullName(), user.getEmail(),
                company != null ? company.getId() : null, company != null ? company.getCompanyCode() : null,
                company != null ? company.getName() : null, roles, permissions);
    }

    @Override
    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash()))
            throw new BusinessException("Current password is incorrect");
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        auditLogService.log("USER", "PASSWORD_CHANGE", user.getId(), user.getUsername(), username, "Self-service");
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        // Always respond the same way regardless of whether the account exists or is active —
        // returning a distinct error here would let an attacker enumerate valid usernames.
        userRepository.findByUsername(request.username())
                .filter(User::isActive)
                .ifPresent(user -> {
                    resetTokenRepository.deleteByUserId(user.getId());
                    String token = UUID.randomUUID().toString();
                    PasswordResetToken prt = PasswordResetToken.builder()
                            .user(user).token(token).expiresAt(Instant.now().plus(24, ChronoUnit.HOURS)).build();
                    resetTokenRepository.save(prt);
                    emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), token);
                });
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken prt = resetTokenRepository.findByToken(request.token())
                .orElseThrow(() -> new BusinessException("Invalid or expired reset link."));
        if (prt.isUsed()) throw new BusinessException("This reset link has already been used.");
        if (prt.isExpired()) throw new BusinessException("This reset link has expired.");
        User user = prt.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        prt.setUsed(true);
        userRepository.save(user);
        resetTokenRepository.save(prt);
        auditLogService.log("USER", "PASSWORD_CHANGE", user.getId(), user.getUsername(), user.getUsername(), "Reset via email");
    }

    @Override
    @Transactional
    public MfaSetupResponse setupMfa(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));
        MfaService.MfaSetupResult result = mfaService.generateSecret(username);
        user.setMfaSecret(result.secret());
        userRepository.save(user);
        return new MfaSetupResponse(result.qrDataUri(), result.secret());
    }

    @Override
    @Transactional
    public void enableMfa(String username, String code) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));
        if (user.getMfaSecret() == null) throw new BusinessException("Call setup-mfa first");
        if (!mfaService.verifyCode(user.getMfaSecret(), code))
            throw new BusinessException("Invalid OTP — please retry", HttpStatus.UNAUTHORIZED);
        user.setMfaEnabled(true);
        userRepository.save(user);
        auditLogService.log("AUTH", "MFA_ENABLED", user.getId(), username, username, null);
    }

    @Override
    @Transactional
    public void disableMfa(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));
        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        userRepository.save(user);
        auditLogService.log("AUTH", "MFA_DISABLED", user.getId(), username, username, null);
    }

    @Override
    @Transactional(readOnly = true)
    public TokenResponse confirmMfa(MfaConfirmRequest request) {
        final Claims claims;
        try {
            claims = jwtService.parse(request.mfaPendingToken());
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BusinessException("Invalid or expired MFA session", HttpStatus.UNAUTHORIZED);
        }
        if (!jwtService.isMfaPendingToken(claims))
            throw new BusinessException("Invalid token type", HttpStatus.UNAUTHORIZED);
        String username = claims.getSubject();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("User not found", HttpStatus.UNAUTHORIZED));
        if (!mfaService.verifyCode(user.getMfaSecret(), request.code())) {
            auditLogService.log("AUTH", "MFA_FAILED", user.getId(), username, username, "Wrong OTP");
            throw new BusinessException("Invalid OTP", HttpStatus.UNAUTHORIZED);
        }
        AppUserPrincipal principal = new AppUserPrincipal(user);
        List<String> authorities = principal.getAuthorityStrings().stream().sorted().toList();
        Long companyId = user.getCompany() != null ? user.getCompany().getId() : null;
        String access = jwtService.generateAccessToken(username, user.getId(), companyId, authorities);
        String refresh = jwtService.generateRefreshToken(username, user.getId());
        auditLogService.log("AUTH", "LOGIN", user.getId(), username, username, "MFA confirmed");
        TokenResponse response = TokenResponse.bearer(access, refresh, jwtService.getAccessExpirySeconds());
        sessionTrackingService.registerSession(user.getId(), username, access);
        return response;
    }

    @Override
    public void logout(String accessToken, String refreshToken) {
        try {
            Claims claims = jwtService.parse(accessToken);
            tokenBlacklistService.blacklist(accessToken, claims.getExpiration());
            Object uid = claims.get("uid");
            if (uid != null) sessionTrackingService.removeSession(Long.valueOf(uid.toString()));
            auditLogService.log("AUTH", "LOGOUT", null, claims.getSubject(), claims.getSubject(), null);
        } catch (Exception ex) {
            log.warn("Logout called with invalid token: {}", ex.getMessage());
        }
        if (refreshToken != null && !refreshToken.isBlank()) {
            try {
                Claims refreshClaims = jwtService.parse(refreshToken);
                tokenBlacklistService.blacklist(refreshToken, refreshClaims.getExpiration());
            } catch (Exception ex) {
                log.warn("Logout called with invalid refresh token: {}", ex.getMessage());
            }
        }
    }
}
