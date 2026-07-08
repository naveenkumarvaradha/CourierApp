package com.courierapp.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis-backed account lockout.
 * After MAX_ATTEMPTS failed logins the account is locked for LOCK_DURATION.
 * Keys survive server restarts (unlike in-memory maps).
 */
@Slf4j
@Service
public class AccountLockoutService {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration ATTEMPT_WINDOW = Duration.ofMinutes(15);
    private static final Duration LOCK_DURATION  = Duration.ofMinutes(30);

    private static final String ATTEMPTS_KEY = "lockout:attempts:";
    private static final String LOCKED_KEY   = "lockout:locked:";

    private final RedisTemplate<String, String> redis;

    public AccountLockoutService(RedisTemplate<String, String> redis) {
        this.redis = redis;
    }

    /** Call on every failed login. Returns true if the account is now locked. */
    public boolean recordFailure(String username) {
        String attKey = ATTEMPTS_KEY + username.toLowerCase();
        Long count = redis.opsForValue().increment(attKey);
        if (count != null && count == 1) {
            redis.expire(attKey, ATTEMPT_WINDOW);
        }
        if (count != null && count >= MAX_ATTEMPTS) {
            redis.opsForValue().set(LOCKED_KEY + username.toLowerCase(), "1", LOCK_DURATION);
            redis.delete(attKey);
            log.warn("Account '{}' locked for {} minutes after {} failed attempts",
                    username, LOCK_DURATION.toMinutes(), MAX_ATTEMPTS);
            return true;
        }
        return false;
    }

    /** Call on successful login to clear failure counter. */
    public void clearFailures(String username) {
        redis.delete(ATTEMPTS_KEY + username.toLowerCase());
        redis.delete(LOCKED_KEY + username.toLowerCase());
    }

    /** Returns true if the account is currently locked out. */
    public boolean isLocked(String username) {
        return Boolean.TRUE.equals(redis.hasKey(LOCKED_KEY + username.toLowerCase()));
    }

    /** Returns remaining lock seconds (0 if not locked). */
    public long lockRemainingSeconds(String username) {
        Duration ttl = redis.getExpire(LOCKED_KEY + username.toLowerCase(),
                java.util.concurrent.TimeUnit.SECONDS) > 0
                ? Duration.ofSeconds(redis.getExpire(LOCKED_KEY + username.toLowerCase(),
                        java.util.concurrent.TimeUnit.SECONDS))
                : Duration.ZERO;
        return ttl.getSeconds();
    }
}
