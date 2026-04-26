package com.digvijay.bookMyShow.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Enables Spring's annotation-driven cache management (@Cacheable, @CacheEvict).

 * Spring Boot auto-configures CaffeineCacheManager when:
 *   1. spring-boot-starter-cache is on the classpath
 *   2. com.github.ben-manes.caffeine:caffeine is on the classpath
 *   3. spring.cache.type=caffeine is set in application.yml
 */

@Configuration
@EnableCaching
public class CacheConfig {
    // Intentionally empty — all Caffeine tuning is in application.yml.
}