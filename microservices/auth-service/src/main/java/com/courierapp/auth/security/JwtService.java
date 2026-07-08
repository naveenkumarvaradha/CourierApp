package com.courierapp.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

@Service
public class JwtService {

    private final SecretKey key;
    private final long accessExpiryMs;
    private final long refreshExpiryMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-expiry-minutes}") long accessMinutes,
            @Value("${app.jwt.refresh-token-expiry-days}") long refreshDays) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpiryMs = accessMinutes * 60_000L;
        this.refreshExpiryMs = refreshDays * 24L * 60L * 60_000L;
    }

    public String generateAccessToken(String username, Long userId, Long companyId, List<String> authorities) {
        Date now = new Date();
        return Jwts.builder()
                .subject(username)
                .claim("uid", userId)
                .claim("cid", companyId)
                .claim("authorities", authorities)
                .claim("type", "access")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessExpiryMs))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(String username, Long userId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(username)
                .claim("uid", userId)
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + refreshExpiryMs))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public String extractUsername(String token) {
        return parse(token).getSubject();
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(parse(token));
    }

    public String generateMfaPendingToken(String username, Long userId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(username)
                .claim("uid", userId)
                .claim("type", "mfa_pending")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + 5 * 60_000L)) // 5-minute window to enter OTP
                .signWith(key)
                .compact();
    }

    public boolean isAccessToken(Claims claims) {
        return "access".equals(claims.get("type", String.class));
    }

    public boolean isRefreshToken(Claims claims) {
        return "refresh".equals(claims.get("type", String.class));
    }

    public boolean isMfaPendingToken(Claims claims) {
        return "mfa_pending".equals(claims.get("type", String.class));
    }

    public long getAccessExpirySeconds() {
        return accessExpiryMs / 1000L;
    }

    @SuppressWarnings("unchecked")
    public List<String> extractAuthorities(Claims claims) {
        Object raw = claims.get("authorities");
        if (raw instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }
}
