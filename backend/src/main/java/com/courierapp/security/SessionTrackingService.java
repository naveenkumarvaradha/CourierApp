package com.courierapp.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Tracks active user sessions in Redis.
 * One session per user — new login replaces old session.
 * Key: session:user:{userId}  Type: Redis Hash
 */
@Slf4j
@Service
public class SessionTrackingService {

    private static final String PREFIX = "session:user:";

    private final RedisTemplate<String, String> redisTemplate;
    private final TokenBlacklistService blacklistService;
    private final JwtService jwtService;

    public SessionTrackingService(RedisTemplate<String, String> redisTemplate,
                                  TokenBlacklistService blacklistService,
                                  JwtService jwtService) {
        this.redisTemplate = redisTemplate;
        this.blacklistService = blacklistService;
        this.jwtService = jwtService;
    }

    public void registerSession(Long userId, String username, String accessToken) {
        try {
            io.jsonwebtoken.Claims claims = jwtService.parse(accessToken);
            Date expiry = claims.getExpiration();
            long ttlMillis = expiry.getTime() - System.currentTimeMillis();
            if (ttlMillis <= 0) return;

            String key = PREFIX + userId;
            Map<String, String> fields = new HashMap<>();
            fields.put("accessToken", accessToken);
            fields.put("username", username);
            fields.put("userId", String.valueOf(userId));
            fields.put("loginAt", Instant.now().toString());
            fields.put("ip", resolveClientIp());
            fields.put("userAgent", resolveUserAgent());
            fields.put("expiry", expiry.toInstant().toString());

            redisTemplate.opsForHash().putAll(key, fields);
            redisTemplate.expire(key, Duration.ofMillis(ttlMillis));
            log.debug("Session registered for user '{}' (userId={})", username, userId);
        } catch (Exception ex) {
            log.warn("Failed to register session for userId={}: {}", userId, ex.getMessage());
        }
    }

    public void removeSession(Long userId) {
        redisTemplate.delete(PREFIX + userId);
    }

    public List<Map<String, String>> listActiveSessions() {
        try {
            Set<String> keys = redisTemplate.keys(PREFIX + "*");
            if (keys == null || keys.isEmpty()) return Collections.emptyList();

            List<Map<String, String>> sessions = new ArrayList<>();
            for (String key : keys) {
                Map<Object, Object> raw = redisTemplate.opsForHash().entries(key);
                if (raw.isEmpty()) continue;
                Map<String, String> session = new LinkedHashMap<>();
                raw.forEach((k, v) -> {
                    String sk = String.valueOf(k);
                    // Never expose the actual token in the session list
                    if (!"accessToken".equals(sk)) {
                        session.put(sk, String.valueOf(v));
                    }
                });
                // Add TTL info
                Long ttl = redisTemplate.getExpire(key, java.util.concurrent.TimeUnit.SECONDS);
                session.put("expiresInSeconds", ttl != null ? String.valueOf(ttl) : "0");
                sessions.add(session);
            }
            sessions.sort(Comparator.comparing(s -> s.getOrDefault("loginAt", "")));
            return sessions;
        } catch (Exception ex) {
            log.warn("Failed to list active sessions: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }

    public boolean terminateSession(Long userId) {
        String key = PREFIX + userId;
        try {
            Object tokenObj = redisTemplate.opsForHash().get(key, "accessToken");
            Object expiryObj = redisTemplate.opsForHash().get(key, "expiry");
            if (tokenObj != null) {
                Date expiry = expiryObj != null
                        ? Date.from(Instant.parse((String) expiryObj))
                        : new Date(System.currentTimeMillis() + 86_400_000L);
                blacklistService.blacklist((String) tokenObj, expiry);
                log.info("Admin terminated session for userId={}", userId);
            }
            return Boolean.TRUE.equals(redisTemplate.delete(key));
        } catch (Exception ex) {
            log.warn("Failed to terminate session for userId={}: {}", userId, ex.getMessage());
            return false;
        }
    }

    private String resolveClientIp() {
        try {
            var attrs = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            var req = attrs.getRequest();
            String xff = req.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
            String realIp = req.getHeader("X-Real-IP");
            if (realIp != null && !realIp.isBlank()) return realIp;
            return req.getRemoteAddr();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String resolveUserAgent() {
        try {
            var attrs = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            String ua = attrs.getRequest().getHeader("User-Agent");
            if (ua == null) return "";
            return ua.length() > 200 ? ua.substring(0, 200) : ua;
        } catch (Exception e) {
            return "";
        }
    }
}
