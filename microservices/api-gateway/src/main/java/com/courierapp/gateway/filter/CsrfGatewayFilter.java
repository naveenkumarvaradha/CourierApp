package com.courierapp.gateway.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;

/**
 * Double-submit-cookie CSRF protection. Now that auth relies on ambient httpOnly cookies
 * instead of a JS-attached Authorization header, browsers will send the auth cookie on
 * any cross-site request too — SameSite=Lax/Strict on the auth cookies already blocks most
 * of that, but this adds an explicit, defense-in-depth check for state-changing requests:
 * a non-httpOnly XSRF-TOKEN cookie is issued, and mutating requests must echo its value back
 * in a header, which a cross-site page cannot read (browsers don't let JS on siteA.com read
 * cookies set for siteB.com).
 */
@Component
public class CsrfGatewayFilter implements GlobalFilter, Ordered {

    private static final String COOKIE_NAME = "XSRF-TOKEN";
    private static final String HEADER_NAME = "X-XSRF-TOKEN";
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    // Credential/token-based endpoints that don't rely on an ambient auth cookie to
    // authorize the action, so there's nothing for a forged cross-site request to hijack.
    private static final Set<String> EXEMPT_PATHS = Set.of(
        "/api/auth/login",
        "/api/auth/forgot-password",
        "/api/auth/reset-password",
        "/api/auth/reset-password/validate"
    );

    @Value("${app.cookie.secure:true}")
    private boolean cookieSecure;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        if (path.contains("/actuator")) {
            return chain.filter(exchange);
        }

        HttpCookie existing = request.getCookies().getFirst(COOKIE_NAME);
        String token = (existing != null && !existing.getValue().isBlank())
                ? existing.getValue() : UUID.randomUUID().toString();

        if (existing == null || existing.getValue().isBlank()) {
            ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, token)
                    .httpOnly(false) // must be JS-readable so the frontend can echo it back
                    .secure(cookieSecure)
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(Duration.ofDays(1))
                    .build();
            exchange.getResponse().addCookie(cookie);
        }

        String method = request.getMethod().name();
        if (!SAFE_METHODS.contains(method) && !isExempt(path)) {
            String headerToken = request.getHeaders().getFirst(HEADER_NAME);
            if (headerToken == null || !MessageDigest.isEqual(
                    headerToken.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8))) {
                return reject(exchange);
            }
        }

        return chain.filter(exchange);
    }

    private boolean isExempt(String path) {
        for (String exempt : EXEMPT_PATHS) {
            if (path.equals(exempt) || path.startsWith(exempt + "/")) {
                return true;
            }
        }
        return false;
    }

    private Mono<Void> reject(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");
        String body = "{\"error\":\"Forbidden\",\"message\":\"Missing or invalid CSRF token\"}";
        var buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return 0; // after JwtAuthGatewayFilter (-1), so an invalid/missing JWT is reported first
    }
}
