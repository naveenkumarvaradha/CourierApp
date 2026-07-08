package com.courierapp.service.impl;

import com.courierapp.dto.auth.*;
import com.courierapp.entity.Company;
import com.courierapp.entity.PasswordResetToken;
import com.courierapp.entity.User;
import com.courierapp.exception.BusinessException;
import com.courierapp.exception.ResourceNotFoundException;
import com.courierapp.repository.CompanyRepository;
import com.courierapp.repository.PasswordResetTokenRepository;
import com.courierapp.repository.UserRepository;
import com.courierapp.security.AppUserPrincipal;
import com.courierapp.security.JwtService;
import com.courierapp.service.AuditLogService;
import com.courierapp.service.AuthService;
import com.courierapp.service.EmailService;
import com.courierapp.security.SessionTrackingService;
import com.courierapp.service.MfaService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
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
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final EmailService emailService;
    private final com.courierapp.security.TokenBlacklistService tokenBlacklistService;
    private final MfaService mfaService;
    private final SessionTrackingService sessionTrackingService;
    private final com.courierapp.security.AccountLockoutService accountLockoutService;
    private final com.courierapp.security.PasswordStrengthValidator passwordStrengthValidator;

    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           JwtService jwtService,
                           UserRepository userRepository,
                           CompanyRepository companyRepository,
                           PasswordResetTokenRepository resetTokenRepository,
                           PasswordEncoder passwordEncoder,
                           AuditLogService auditLogService,
                           EmailService emailService,
                           com.courierapp.security.TokenBlacklistService tokenBlacklistService,
                           MfaService mfaService,
                           SessionTrackingService sessionTrackingService,
                           com.courierapp.security.AccountLockoutService accountLockoutService,
                           com.courierapp.security.PasswordStrengthValidator passwordStrengthValidator) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
        this.emailService = emailService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.mfaService = mfaService;
        this.sessionTrackingService = sessionTrackingService;
        this.accountLockoutService = accountLockoutService;
        this.passwordStrengthValidator = passwordStrengthValidator;
    }

    @Override
    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        // Check account lockout before touching DB (fast Redis check)
        if (accountLockoutService.isLocked(request.username())) {
            long secs = accountLockoutService.lockRemainingSeconds(request.username());
            throw new BusinessException("Account is locked. Try again in " + secs + " seconds.", HttpStatus.TOO_MANY_REQUESTS);
        }
        // Verify company exists and is active
        Company company = companyRepository.findByCompanyCodeIgnoreCase(request.companyCode())
                .orElseThrow(() -> new BadCredentialsException("Invalid company code or credentials"));
        if (!company.isActive()) {
            throw new BadCredentialsException("Company account is inactive");
        }
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
            AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();

            // Verify the authenticated user belongs to the selected company
            User user = userRepository.findById(principal.getId())
                    .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
            if (user.getCompany() == null || !user.getCompany().getId().equals(company.getId())) {
                log.warn("User '{}' attempted login under wrong company '{}'", request.username(), request.companyCode());
                accountLockoutService.recordFailure(request.username());
                auditLogService.log("AUTH", "LOGIN_FAILED", principal.getId(), request.username(),
                        request.username(), "Wrong company: " + request.companyCode());
                throw new BadCredentialsException("Invalid company code or credentials");
            }

            // If MFA is enabled AND a secret is actually configured, require OTP
            if (user.isMfaEnabled() && user.getMfaSecret() != null) {
                String pending = jwtService.generateMfaPendingToken(principal.getUsername(), user.getId());
                log.info("User '{}' passed password — awaiting MFA confirmation", request.username());
                return TokenResponse.mfaRequired(pending);
            }

            // Successful login — clear failure counter
            accountLockoutService.clearFailures(request.username());
            List<String> authorities = principal.getAuthorityStrings().stream().sorted().toList();
            String access = jwtService.generateAccessToken(
                    principal.getUsername(), principal.getId(), company.getId(), authorities);
            String refresh = jwtService.generateRefreshToken(principal.getUsername(), principal.getId());
            log.info("User '{}' logged in under company '{}'", request.username(), company.getCompanyCode());
            auditLogService.log("AUTH", "LOGIN", principal.getId(), principal.getUsername(),
                    principal.getUsername(), "Company: " + company.getCompanyCode());
            TokenResponse response = TokenResponse.bearer(access, refresh, jwtService.getAccessExpirySeconds());
            sessionTrackingService.registerSession(principal.getId(), principal.getUsername(), access);
            return response;
        } catch (BadCredentialsException ex) {
            boolean nowLocked = accountLockoutService.recordFailure(request.username());
            log.warn("Failed login attempt for username='{}' company='{}' locked={}", request.username(), request.companyCode(), nowLocked);
            auditLogService.log("AUTH", "LOGIN_FAILED", null, request.username(),
                    request.username(), "Invalid credentials");
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
        if (!jwtService.isRefreshToken(claims)) {
            throw new BusinessException("Provided token is not a refresh token", HttpStatus.UNAUTHORIZED);
        }
        String username = claims.getSubject();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("User no longer exists", HttpStatus.UNAUTHORIZED));
        if (!user.isActive()) {
            throw new BusinessException("User account is disabled", HttpStatus.UNAUTHORIZED);
        }
        AppUserPrincipal principal = new AppUserPrincipal(user);
        List<String> authorities = principal.getAuthorityStrings().stream().sorted().toList();
        Long companyId = user.getCompany() != null ? user.getCompany().getId() : null;
        String access = jwtService.generateAccessToken(username, user.getId(), companyId, authorities);
        String newRefresh = jwtService.generateRefreshToken(username, user.getId());
        return TokenResponse.bearer(access, newRefresh, jwtService.getAccessExpirySeconds());
    }

    @Override
    @Transactional(readOnly = true)
    public CurrentUserResponse currentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));
        List<String> roles = user.getRoles().stream().map(r -> r.getName()).sorted().toList();
        Set<String> permissions = new AppUserPrincipal(user).getAuthorityStrings().stream()
                .filter(a -> !a.startsWith("ROLE_"))
                .collect(Collectors.toCollection(java.util.TreeSet::new));
        Company company = user.getCompany();
        return new CurrentUserResponse(
                user.getId(), user.getUsername(), user.getFullName(), user.getEmail(),
                company != null ? company.getId() : null,
                company != null ? company.getCompanyCode() : null,
                company != null ? company.getName() : null,
                roles, permissions);
    }

    @Override
    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException("Current password is incorrect");
        }
        passwordStrengthValidator.validate(request.newPassword(), username);
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        auditLogService.log("USER", "PASSWORD_CHANGE", user.getId(), user.getUsername(),
                username, "Self-service password change");
        log.info("User '{}' changed their password", username);
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BusinessException("No account found with username '" + request.username() + "'."));
        if (!user.isActive()) {
            throw new BusinessException("This account is inactive. Please contact your administrator.");
        }
        // Invalidate any existing tokens for this user
        resetTokenRepository.deleteByUserId(user.getId());
        // Create new reset token valid for 24 hours
        String token = UUID.randomUUID().toString();
        PasswordResetToken prt = PasswordResetToken.builder()
                .user(user)
                .token(token)
                .expiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
                .build();
        resetTokenRepository.save(prt);
        emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), token);
        log.info("Password reset email sent to '{}' for user '{}'", user.getEmail(), user.getUsername());
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken prt = resetTokenRepository.findByToken(request.token())
                .orElseThrow(() -> new BusinessException("Invalid or expired reset link. Please request a new one."));
        if (prt.isUsed()) {
            throw new BusinessException("This reset link has already been used.");
        }
        if (prt.isExpired()) {
            throw new BusinessException("This reset link has expired. Please request a new one.");
        }
        User user = prt.getUser();
        passwordStrengthValidator.validate(request.newPassword(), user.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        prt.setUsed(true);
        userRepository.save(user);
        resetTokenRepository.save(prt);
        auditLogService.log("USER", "PASSWORD_CHANGE", user.getId(), user.getUsername(),
                user.getUsername(), "Password reset via email link");
        log.info("Password reset completed for user '{}'", user.getUsername());
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
        if (user.getMfaSecret() == null) {
            throw new BusinessException("Call setup-mfa first to generate a secret");
        }
        if (!mfaService.verifyCode(user.getMfaSecret(), code)) {
            throw new BusinessException("Invalid OTP — please scan the QR code again and retry", HttpStatus.UNAUTHORIZED);
        }
        user.setMfaEnabled(true);
        userRepository.save(user);
        auditLogService.log("AUTH", "MFA_ENABLED", user.getId(), username, username, null);
        log.info("MFA enabled for user '{}'", username);
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
        log.info("MFA disabled for user '{}'", username);
    }

    @Override
    @Transactional(readOnly = true)
    public TokenResponse confirmMfa(MfaConfirmRequest request) {
        final Claims claims;
        try {
            claims = jwtService.parse(request.mfaPendingToken());
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BusinessException("Invalid or expired MFA session — please log in again", HttpStatus.UNAUTHORIZED);
        }
        if (!jwtService.isMfaPendingToken(claims)) {
            throw new BusinessException("Invalid token type", HttpStatus.UNAUTHORIZED);
        }
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
        log.info("User '{}' completed MFA login", username);
        TokenResponse response = TokenResponse.bearer(access, refresh, jwtService.getAccessExpirySeconds());
        sessionTrackingService.registerSession(user.getId(), username, access);
        return response;
    }

    @Override
    public void logout(String token) {
        try {
            io.jsonwebtoken.Claims claims = jwtService.parse(token);
            tokenBlacklistService.blacklist(claims.getId(), claims.getExpiration());
            Object userIdClaim = claims.get("uid");
            if (userIdClaim != null) {
                sessionTrackingService.removeSession(Long.valueOf(userIdClaim.toString()));
            }
            log.info("User '{}' logged out — JTI {} blacklisted", claims.getSubject(), claims.getId());
            auditLogService.log("AUTH", "LOGOUT", null, claims.getSubject(), claims.getSubject(), null);
        } catch (Exception ex) {
            log.warn("Logout called with invalid token: {}", ex.getMessage());
        }
    }
}
