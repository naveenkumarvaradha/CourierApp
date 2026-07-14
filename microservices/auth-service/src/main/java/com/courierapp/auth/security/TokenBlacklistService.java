package com.courierapp.auth.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Date;
import java.util.HexFormat;

/**
 * Redis-backed JWT token blacklist.
 * On logout the token (hashed) is stored with TTL = remaining token lifetime.
 * Every authenticated request checks the blacklist — O(1) Redis lookup.
 */
@Slf4j
@Service
public class TokenBlacklistService {

    private static final String PREFIX = "blacklist:jwt:";

    private final RedisTemplate<String, String> redisTemplate;

    public TokenBlacklistService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /** Blacklist a token until its natural expiry. */
    public void blacklist(String token, Date expiry) {
        if (expiry == null) return;
        long ttlMillis = expiry.getTime() - System.currentTimeMillis();
        if (ttlMillis <= 0) return;                 // already expired — no need to store
        String key = PREFIX + hash(token);
        redisTemplate.opsForValue().set(key, "1", Duration.ofMillis(ttlMillis));
        log.debug("Token blacklisted (ttl={}s)", ttlMillis / 1000);
    }

    /** Returns true when the token has been revoked. */
    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + hash(token)));
    }

    /** SHA-256 digest of the token — collision-resistant, unlike Object.hashCode(). */
    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
