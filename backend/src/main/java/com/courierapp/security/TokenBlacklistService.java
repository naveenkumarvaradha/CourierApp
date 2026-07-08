package com.courierapp.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;

/**
 * Redis-backed JWT token blacklist.
 * On logout the token jti (or raw token) is stored with TTL = remaining token lifetime.
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

    /** Blacklist a token by its JTI until its natural expiry. */
    public void blacklist(String jti, Date expiry) {
        if (jti == null || expiry == null) return;
        long ttlMillis = expiry.getTime() - System.currentTimeMillis();
        if (ttlMillis <= 0) return;
        redisTemplate.opsForValue().set(PREFIX + jti, "1", Duration.ofMillis(ttlMillis));
        log.debug("Token JTI {} blacklisted (ttl={}s)", jti, ttlMillis / 1000);
    }

    /** Returns true when the JTI has been revoked. */
    public boolean isBlacklisted(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + jti));
    }
}
