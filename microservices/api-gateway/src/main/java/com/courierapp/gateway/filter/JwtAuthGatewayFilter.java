package com.courierapp.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class JwtAuthGatewayFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthGatewayFilter.class);

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.internal.secret}")
    private String internalSecret;

    private SecretKey signingKey;

    /**
     * Paths that do NOT require a JWT token.
     * These are matched against the raw request path (after the service prefix is kept in place
     * because the gateway routes use StripPrefix=0).
     */
    private static final Set<String> PUBLIC_PATHS = Set.of(
        "/api/auth/login",
        "/api/auth/refresh",
        "/api/auth/confirm-mfa",
        "/api/auth/companies",
        "/api/auth/forgot-password",
        "/api/auth/reset-password",
        "/api/auth/reset-password/validate"
    );

    /**
     * Identity/internal headers that must only ever be set by this gateway. Any of these
     * arriving from a client are stripped before routing so a caller cannot smuggle a
     * spoofed identity past the JWT check and have it trusted by a downstream service.
     */
    private static final Set<String> INTERNAL_HEADERS = Set.of(
        "X-User-Id", "X-Username", "X-Company-Id", "X-Roles", "X-Internal-Auth", "X-Internal-Service"
    );

    @PostConstruct
    public void init() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            // Pad the key to at least 256 bits (required by HMAC-SHA256)
            byte[] paddedKey = new byte[32];
            System.arraycopy(keyBytes, 0, paddedKey, 0, Math.min(keyBytes.length, 32));
            keyBytes = paddedKey;
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        // Strip any client-supplied identity/internal headers on every request, regardless of
        // path, so a caller can never smuggle a spoofed identity or internal-auth secret past
        // this gateway and have it trusted by a downstream service.
        ServerHttpRequest strippedRequest = exchange.getRequest().mutate()
            .headers(headers -> INTERNAL_HEADERS.forEach(headers::remove))
            .build();
        exchange = exchange.mutate().request(strippedRequest).build();

        // Actuator endpoints — always allow without authentication
        if (path.contains("/actuator")) {
            return chain.filter(exchange);
        }

        // Public auth endpoints — no JWT required
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        // All other paths require a valid access token — read from the httpOnly cookie the
        // browser sends automatically; fall back to an Authorization header for tooling/tests.
        String token = extractToken(exchange.getRequest());
        if (token == null) {
            return unauthorize(exchange, "Missing or invalid access token");
        }

        try {
            Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

            // Reject tokens that are only valid for MFA confirmation
            String tokenType = claims.get("type", String.class);
            if ("mfa_pending".equals(tokenType)) {
                return unauthorize(exchange, "MFA verification required");
            }

            // Propagate user context to downstream services via headers, signed with a
            // shared secret so services can verify the headers actually came from this
            // gateway and not from a caller with direct network access to their port.
            ServerHttpRequest enriched = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.set("X-User-Id",      claims.get("uid")   != null ? claims.get("uid").toString()  : "");
                    headers.set("X-Username",     claims.getSubject() != null ? claims.getSubject()            : "");
                    headers.set("X-Company-Id",   claims.get("cid")   != null ? claims.get("cid").toString()  : "");
                    headers.set("X-Roles",        extractRolesHeader(claims));
                    headers.set("X-Internal-Auth", internalSecret);
                })
                .build();

            return chain.filter(exchange.mutate().request(enriched).build());

        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT validation failed for path {}: {}", path, e.getMessage());
            return unauthorize(exchange, "Invalid or expired token");
        }
    }

    /** Prefers the httpOnly access_token cookie; falls back to an Authorization header for tooling. */
    private String extractToken(ServerHttpRequest request) {
        var cookie = request.getCookies().getFirst("access_token");
        if (cookie != null && !cookie.getValue().isBlank()) {
            return cookie.getValue();
        }
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    /**
     * The JWT stores authorities as a JSON array under the "authorities" claim (see
     * JwtService.generateAccessToken), which JJWT deserializes as a List — join it into a
     * clean comma-separated string rather than relying on List.toString()'s "[a, b]" format,
     * which HeaderAuthenticationFilter's naive split(",") would otherwise mangle.
     */
    private String extractRolesHeader(Claims claims) {
        Object raw = claims.get("authorities");
        if (raw instanceof List<?> list) {
            return list.stream().map(String::valueOf).collect(Collectors.joining(","));
        }
        return raw != null ? raw.toString() : "";
    }

    /**
     * Returns true when the request path matches one of the declared public paths.
     * Matching is prefix-based so sub-paths (e.g. /api/auth/reset-password/abc123) are also allowed.
     */
    private boolean isPublicPath(String path) {
        for (String publicPath : PUBLIC_PATHS) {
            if (path.equals(publicPath) || path.startsWith(publicPath + "/") || path.startsWith(publicPath + "?")) {
                return true;
            }
        }
        return false;
    }

    private Mono<Void> unauthorize(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");
        String body = "{\"error\":\"Unauthorized\",\"message\":\"" + message + "\"}";
        var buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -1; // Execute before all other filters
    }
}
