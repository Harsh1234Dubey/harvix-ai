package com.interviewai.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewai.common.response.ApiError;
import com.interviewai.common.util.SecurityUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final long capacity;
    private final long refillPerMinute;

    public RateLimitFilter(ObjectMapper objectMapper,
                           @Value("${app.security.rate-limit.capacity:120}") long capacity,
                           @Value("${app.security.rate-limit.refill-per-minute:60}") long refillPerMinute) {
        this.objectMapper = objectMapper;
        this.capacity = capacity;
        this.refillPerMinute = refillPerMinute;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui")
                || path.startsWith("/actuator") || path.startsWith("/api/v1/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String key = clientKey(request);
        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket(capacity, refillPerMinute));
        if (!bucket.tryAcquire()) {
            ApiError error = ApiError.builder()
                    .timestamp(Instant.now())
                    .status(429)
                    .error("Too Many Requests")
                    .message("Too many requests. Please slow down and retry.")
                    .path(request.getRequestURI())
                    .build();
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(error));
            return;
        }
        filterChain.doFilter(request, response);
    }

    private String clientKey(HttpServletRequest request) {
        String user = SecurityUtils.currentUserEmail();
        return user != null ? "u:" + user : "ip:" + SecurityUtils.clientIp(request);
    }

    private static final class Bucket {
        private final long refillPerSecond;
        private final long capacity;
        private final AtomicLong tokens;
        private volatile long lastRefill;

        Bucket(long capacity, long refillPerMinute) {
            this.capacity = capacity;
            this.refillPerSecond = Math.max(1, refillPerMinute / 60);
            this.tokens = new AtomicLong(capacity);
            this.lastRefill = System.nanoTime();
        }

        boolean tryAcquire() {
            refill();
            return tokens.decrementAndGet() >= 0;
        }

        private void refill() {
            long now = System.nanoTime();
            long elapsedNanos = now - lastRefill;
            long refilled = (long) (elapsedNanos / 1_000_000_000.0 * refillPerSecond);
            if (refilled > 0) {
                lastRefill = now;
                tokens.accumulateAndGet(refilled, (cur, add) -> Math.min(cur + add, capacity));
            }
        }
    }
}
