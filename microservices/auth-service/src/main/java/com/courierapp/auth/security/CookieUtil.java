package com.courierapp.auth.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Builds the httpOnly auth cookies. Secure defaults to true (requires HTTPS); set
 * COOKIE_SECURE=false only for local HTTP-only dev (e.g. `mvn spring-boot:run` without TLS).
 */
@Component
public class CookieUtil {

    public static final String ACCESS_COOKIE = "access_token";
    public static final String REFRESH_COOKIE = "refresh_token";
    public static final String MFA_PENDING_COOKIE = "mfa_pending";

    @Value("${app.cookie.secure:true}")
    private boolean secure;

    public ResponseCookie build(String name, String value, long maxAgeSeconds, String path, String sameSite) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(path)
                .maxAge(maxAgeSeconds)
                .build();
    }

    public ResponseCookie clear(String name, String path) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path(path)
                .maxAge(0)
                .build();
    }
}
