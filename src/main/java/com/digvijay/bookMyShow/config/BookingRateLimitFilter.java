package com.digvijay.bookMyShow.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-IP rate limiter applied exclusively to POST /api/bookings/** endpoints.
 *
 * Algorithm: Token Bucket (Bucket4j)
 *   - Each unique client IP gets its own bucket.
 *   - Bucket capacity  = rate-limit.booking.capacity     (default: 10)
 *   - Refill rate      = rate-limit.booking.refill-tokens every
 *                        rate-limit.booking.refill-minutes (default: 10/min)
 *
 * When limit exceeded:
 *   → HTTP 429 Too Many Requests
 *   → X-RateLimit-Retry-After header tells client when to retry
 *
 * GET /api/bookings/** (history, lookup) is NOT rate-limited — read paths are cheap.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingRateLimitFilter extends OncePerRequestFilter {

    private final RateLimitConfig rateLimitConfig;

    // ConcurrentHashMap: one Bucket per client IP.
    // In a multi-instance deployment, replace with Bucket4j + Redis/Hazelcast.
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Only rate-limit POST/DELETE write operations on /api/bookings/**
        // GET requests (booking history, lookup) pass through freely
        String method = request.getMethod();
        String path   = request.getRequestURI();
        boolean isBookingWrite = path.startsWith("/api/bookings")
                && ("POST".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method));
        return !isBookingWrite;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException, IOException {

        String clientIp = resolveClientIp(request);
        Bucket bucket   = buckets.computeIfAbsent(clientIp, this::newBucket);

        if (bucket.tryConsume(1)) {
            // Token consumed — request proceeds
            long remaining = bucket.getAvailableTokens();
            response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
            chain.doFilter(request, response);
        } else {
            // Bucket empty — reject with 429
            long waitSeconds = bucket.estimateAbilityToConsume(1)
                    .getNanosToWaitForRefill() / 1_000_000_000L;

            log.warn("Rate limit exceeded for IP: {} on {} {}", clientIp,
                    request.getMethod(), request.getRequestURI());

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("X-RateLimit-Retry-After", String.valueOf(waitSeconds));
            response.getWriter().write("""
                    {
                      "status": 429,
                      "message": "Too many booking requests. You are allowed %d requests per %d minute(s). Retry after %d second(s).",
                      "retryAfterSeconds": %d
                    }
                    """.formatted(
                    rateLimitConfig.getCapacity(),
                    rateLimitConfig.getRefillMinutes(),
                    waitSeconds,
                    waitSeconds));
        }
    }

    /** Creates a new token-bucket for a client IP using config values. */
    private Bucket newBucket(String ip) {
        Refill refill = Refill.greedy(
                rateLimitConfig.getRefillTokens(),
                Duration.ofMinutes(rateLimitConfig.getRefillMinutes())
        );
        Bandwidth limit = Bandwidth.classic(rateLimitConfig.getCapacity(), refill);
        log.debug("Created rate-limit bucket for IP: {} — capacity: {}, refill: {}/{} min",
                ip, rateLimitConfig.getCapacity(),
                rateLimitConfig.getRefillTokens(), rateLimitConfig.getRefillMinutes());
        return Bucket.builder().addLimit(limit).build();
    }

    /**
     * Resolves the real client IP, honouring X-Forwarded-For when behind a proxy/load-balancer.
     * Falls back to getRemoteAddr() for direct connections.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // X-Forwarded-For may be a comma-separated list; first entry is the original client
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
