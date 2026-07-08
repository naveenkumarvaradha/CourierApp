package com.courierapp.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory rate limiter for the /auth/login endpoint.
 * Limits each IP to 10 attempts per minute; blocks for 5 minutes after that.
 * No external dependency required.
 */
@Slf4j
@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_ATTEMPTS = 10;
    private static final long WINDOW_MS   = 60_000L;   // 1 minute
    private static final long BLOCK_MS    = 300_000L;  // 5 minutes

    private static final class Bucket {
        final AtomicInteger count;
        final long windowStart;
        volatile long blockedUntil;
        Bucket(AtomicInteger count, long windowStart, long blockedUntil) {
            this.count = count; this.windowStart = windowStart; this.blockedUntil = blockedUntil;
        }
    }

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getServletPath().endsWith("/auth/login")
                || !"POST".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String ip = resolveIp(request);
        long now = Instant.now().toEpochMilli();

        Bucket bucket = buckets.compute(ip, (k, existing) -> {
            if (existing == null || now - existing.windowStart > WINDOW_MS) {
                return new Bucket(new AtomicInteger(0), now, 0L);
            }
            return existing;
        });

        if (bucket.blockedUntil > now) {
            long secondsLeft = (bucket.blockedUntil - now) / 1000;
            log.warn("Rate limit: blocked login attempt from IP {} ({} seconds remaining)", ip, secondsLeft);
            reject(response, "Too many login attempts. Try again in " + secondsLeft + " seconds.");
            return;
        }

        int attempts = bucket.count.incrementAndGet();
        if (attempts > MAX_ATTEMPTS) {
            bucket.blockedUntil = now + BLOCK_MS;
            log.warn("Rate limit: IP {} exceeded {} attempts - blocked for 5 minutes", ip, MAX_ATTEMPTS);
            reject(response, "Too many login attempts. Blocked for 5 minutes.");
            return;
        }

        chain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }

    private String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
