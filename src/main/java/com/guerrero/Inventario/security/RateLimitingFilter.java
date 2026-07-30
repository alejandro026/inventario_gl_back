package com.guerrero.Inventario.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guerrero.Inventario.dto.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final ConcurrentHashMap<String, TokenBucket> criticalBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, TokenBucket> generalBuckets = new ConcurrentHashMap<>();
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String ip = getClientIP(request);

        // Limites:
        // Criticos: /api/auth/login, /api/auth/refresh -> 5 req/min
        // Generales: /api/** -> 100 req/min
        if (path.startsWith("/api/auth/login") || path.startsWith("/api/auth/refresh")) {
            TokenBucket bucket = criticalBuckets.computeIfAbsent(ip, k -> new TokenBucket(5, 5.0 / 60.0));
            if (!bucket.tryConsume()) {
                sendRateLimitError(request, response, bucket.getRetryAfterSeconds());
                return;
            }
            response.setHeader("X-RateLimit-Remaining", String.valueOf(bucket.getRemainingTokens()));
        } else if (path.startsWith("/api/")) {
            TokenBucket bucket = generalBuckets.computeIfAbsent(ip, k -> new TokenBucket(100, 100.0 / 60.0));
            if (!bucket.tryConsume()) {
                sendRateLimitError(request, response, bucket.getRetryAfterSeconds());
                return;
            }
            response.setHeader("X-RateLimit-Remaining", String.valueOf(bucket.getRemainingTokens()));
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty() || "unknown".equalsIgnoreCase(xfHeader)) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }

    private void sendRateLimitError(HttpServletRequest request, HttpServletResponse response, long retryAfter) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", String.valueOf(retryAfter));

        ApiError body = ApiError.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .error(HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase())
                .message("Demasiadas peticiones. Por favor intente de nuevo en " + retryAfter + " segundos.")
                .path(request.getRequestURI())
                .build();

        mapper.writeValue(response.getWriter(), body);
    }

    private static class TokenBucket {
        private final double capacity;
        private final double refillRatePerSecond;
        private double tokens;
        private long lastRefillTimestamp;

        public TokenBucket(double capacity, double refillRatePerSecond) {
            this.capacity = capacity;
            this.refillRatePerSecond = refillRatePerSecond;
            this.tokens = capacity;
            this.lastRefillTimestamp = System.currentTimeMillis();
        }

        public synchronized boolean tryConsume() {
            refill();
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            double deltaSeconds = (now - lastRefillTimestamp) / 1000.0;
            if (deltaSeconds > 0) {
                tokens = Math.min(capacity, tokens + deltaSeconds * refillRatePerSecond);
                lastRefillTimestamp = now;
            }
        }

        public synchronized long getRetryAfterSeconds() {
            refill();
            if (tokens >= 1.0) {
                return 0;
            }
            double needed = 1.0 - tokens;
            return (long) Math.ceil(needed / refillRatePerSecond);
        }

        public synchronized int getRemainingTokens() {
            refill();
            return (int) Math.floor(tokens);
        }
    }
}
