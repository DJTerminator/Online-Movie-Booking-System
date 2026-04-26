package com.digvijay.bookMyShow.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Binds rate-limit.booking.* from application.yml.
 *
 * Defaults (applied if yml key is absent):
 *   capacity      = 10   (bucket size)
 *   refillTokens  = 10   (tokens added per interval)
 *   refillMinutes = 1    (interval length)
 *
 * Effective limit: 10 requests per minute per IP on /api/bookings/**
 */
@Configuration
@ConfigurationProperties(prefix = "rate-limit.booking")
@Data
public class RateLimitConfig {
    private int capacity     = 10;
    private int refillTokens = 10;
    private int refillMinutes = 1;
}
