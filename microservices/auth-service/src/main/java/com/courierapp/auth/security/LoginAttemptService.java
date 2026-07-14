package com.courierapp.auth.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis-backed failed-login tracker. Locks out an account (by company+username, not by
 * caller IP, since IPs are shared/NATed) after too many consecutive bad credentials.
 */
@Slf4j
@Service
public class LoginAttemptService {

    private static final String PREFIX = "login:fail:";

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${app.login.max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Value("${app.login.lockout-minutes:15}")
    private int lockoutMinutes;

    public LoginAttemptService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isLocked(String key) {
        String value = redisTemplate.opsForValue().get(PREFIX + key);
        return value != null && Integer.parseInt(value) >= maxFailedAttempts;
    }

    public void recordFailure(String key) {
        String redisKey = PREFIX + key;
        Long count = redisTemplate.opsForValue().increment(redisKey);
        if (count != null && count == 1L) {
            redisTemplate.expire(redisKey, Duration.ofMinutes(lockoutMinutes));
        }
    }

    public void recordSuccess(String key) {
        redisTemplate.delete(PREFIX + key);
    }
}
