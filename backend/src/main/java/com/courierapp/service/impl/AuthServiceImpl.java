package com.courierapp.service.impl;

import com.courierapp.dto.auth.CurrentUserResponse;
import com.courierapp.dto.auth.LoginRequest;
import com.courierapp.dto.auth.RefreshRequest;
import com.courierapp.dto.auth.TokenResponse;
import com.courierapp.entity.User;
import com.courierapp.exception.BusinessException;
import com.courierapp.exception.ResourceNotFoundException;
import com.courierapp.repository.UserRepository;
import com.courierapp.security.AppUserPrincipal;
import com.courierapp.security.JwtService;
import com.courierapp.service.AuthService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           JwtService jwtService,
                           UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
        List<String> authorities = principal.getAuthorityStrings().stream().sorted().toList();
        String access = jwtService.generateAccessToken(principal.getUsername(), principal.getId(), authorities);
        String refresh = jwtService.generateRefreshToken(principal.getUsername(), principal.getId());
        return TokenResponse.bearer(access, refresh, jwtService.getAccessExpirySeconds());
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
        String access = jwtService.generateAccessToken(username, user.getId(), authorities);
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
        return new CurrentUserResponse(user.getId(), user.getUsername(), user.getFullName(),
                user.getEmail(), roles, permissions);
    }
}
