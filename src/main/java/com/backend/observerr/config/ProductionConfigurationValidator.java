package com.backend.observerr.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class ProductionConfigurationValidator {
    private static final String DEVELOPMENT_SECRET = "observerr-local-dev-secret-min-32-chars";

    private final Environment environment;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${auth.cookie.secure:false}")
    private boolean secureCookies;

    @Value("${spring.data.redis.url:}")
    private String redisUrl;

    @PostConstruct
    void validate() {
        boolean production = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> profile.equalsIgnoreCase("prod")
                        || profile.equalsIgnoreCase("production"));
        if (!production) {
            return;
        }
        if (jwtSecret == null || jwtSecret.length() < 32 || DEVELOPMENT_SECRET.equals(jwtSecret)) {
            throw new IllegalStateException(
                    "Production requires a unique JWT_SECRET of at least 32 characters");
        }
        if (!secureCookies) {
            throw new IllegalStateException("Production requires AUTH_COOKIE_SECURE=true");
        }
        if (redisUrl == null || redisUrl.isBlank()) {
            throw new IllegalStateException(
                    "Production requires REDIS_URL for distributed token invalidation and rate limits");
        }
    }
}
