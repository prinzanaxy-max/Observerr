package com.backend.observerr.config;

import com.backend.observerr.auth.service.InMemoryRefreshTokenBlocklistService;
import com.backend.observerr.auth.service.RedisRefreshTokenBlocklistService;
import com.backend.observerr.auth.service.RefreshTokenBlocklistService;
import com.backend.observerr.security.InMemoryRateLimitService;
import com.backend.observerr.security.RateLimitService;
import com.backend.observerr.security.RedisRateLimitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Wires Redis-backed services when REDIS_URL is reachable; otherwise falls back
 * to process-local implementations so the app can still bind and pass /health.
 */
@Slf4j
@Configuration
public class DistributedServicesConfig {

    @Bean(destroyMethod = "shutdown")
    @Primary
    RateLimitService rateLimitService(@Value("${spring.data.redis.url:}") String redisUrl) {
        if (redisUrl == null || redisUrl.isBlank()) {
            log.warn("REDIS_URL unset - using in-memory rate limiter");
            return new InMemoryRateLimitService();
        }
        try {
            RedisClientFactory.ConnectionHandle handle = RedisClientFactory.connect(redisUrl.trim());
            log.info("Using Redis rate limiter");
            return new RedisRateLimitService(handle);
        } catch (RuntimeException ex) {
            log.error("Redis rate limiter unavailable ({}), falling back to in-memory: {}",
                    redisUrl.replaceAll(":[^:@/]+@", ":***@"), ex.getMessage());
            return new InMemoryRateLimitService();
        }
    }

    @Bean(destroyMethod = "shutdown")
    @Primary
    RefreshTokenBlocklistService refreshTokenBlocklistService(
            @Value("${spring.data.redis.url:}") String redisUrl) {
        if (redisUrl == null || redisUrl.isBlank()) {
            log.warn("REDIS_URL unset - using in-memory refresh-token blocklist");
            return new InMemoryRefreshTokenBlocklistService();
        }
        try {
            RedisClientFactory.ConnectionHandle handle = RedisClientFactory.connect(redisUrl.trim());
            log.info("Using Redis refresh-token blocklist");
            return new RedisRefreshTokenBlocklistService(handle);
        } catch (RuntimeException ex) {
            log.error("Redis blocklist unavailable ({}), falling back to in-memory: {}",
                    redisUrl.replaceAll(":[^:@/]+@", ":***@"), ex.getMessage());
            return new InMemoryRefreshTokenBlocklistService();
        }
    }
}
