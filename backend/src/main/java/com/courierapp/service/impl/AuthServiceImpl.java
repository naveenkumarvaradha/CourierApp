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

    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           JwtService jwtService,
                           UserRepository userRepository,
                           CompanyRepository companyRepository,
                           PasswordResetTokenRepository resetTokenRepository,
                           PasswordEncoder passwordEncoder,
                           AuditLogService auditLogService,
                           EmailService emailService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
        this.emailService = emailService;
    }

    @Override
    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
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
                auditLogService.log("AUTH", "LOGIN_FAILED", principal.getId(), request.username(),
                        request.username(), "Wrong company: " + request.companyCode());
                throw new BadCredentialsException("Invalid company code or credentials");
            }

            List<String> authorities = principal.getAuthorityStrings().stream().sorted().toList();
            String access = jwtService.generateAccessToken(
                    principal.getUsername(), principal.getId(), company.getId(), authorities);
            String refresh = jwtService.generateRefreshToken(principal.getUsername(), principal.getId());
            log.info("User '{}' logged in under company '{}'", request.username(), company.getCompanyCode());
            auditLogService.log("AUTH", "LOGIN", principal.getId(), principal.getUsername(),
                    principal.getUsername(), "Company: " + company.getCompanyCode());
            return TokenResponse.bearer(access, refresh, jwtService.getAccessExpirySeconds());
        } catch (BadCredentialsException ex) {
            log.warn("Failed login attempt for username='{}' company='{}'", request.username(), request.companyCode());
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
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        auditLogService.log("USER", "PASSWORD_CHANGE", user.getId(), user.getUsername(),
                username, "Self-service password change");
        log.info("User '{}' changed their password", username);
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        // Always return success to avoid user enumeration
        userRepository.findByEmail(request.email()).ifPresent(user -> {
            if (!user.isActive()) return;
            // Invalidate old tokens
            resetTokenRepository.deleteByUserId(user.getId());
            // Create new token
            String token = UUID.randomUUID().toString();
            PasswordResetToken prt = PasswordResetToken.builder()
                    .user(user)
                    .token(token)
                    .expiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
                    .build();
            resetTokenRepository.save(prt);
            emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), token);
            log.info("Password reset email requested for user '{}'", user.getUsername());
        });
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
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        prt.setUsed(true);
        userRepository.save(user);
        resetTokenRepository.save(prt);
        auditLogService.log("USER", "PASSWORD_CHANGE", user.getId(), user.getUsername(),
                user.getUsername(), "Password reset via email link");
        log.info("Password reset completed for user '{}'", user.getUsername());
    }
}
