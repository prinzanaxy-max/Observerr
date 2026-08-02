package com.backend.observerr.security;

import com.backend.observerr.exception.RateLimitExceededException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RequestRateLimiter {

    private final RateLimitService rateLimitService;

    @Value("${rate-limit.auth.max-attempts:20}")
    private int authMaxAttempts;
    @Value("${rate-limit.auth.window-seconds:60}")
    private long authWindowSeconds;
    @Value("${rate-limit.session.max-attempts:10}")
    private int sessionMaxAttempts;
    @Value("${rate-limit.session.window-seconds:60}")
    private long sessionWindowSeconds;
    @Value("${rate-limit.integrity.max-attempts:10}")
    private int integrityMaxAttempts;
    @Value("${rate-limit.integrity.window-seconds:60}")
    private long integrityWindowSeconds;
    @Value("${rate-limit.autosave.max-attempts:120}")
    private int autosaveMaxAttempts;
    @Value("${rate-limit.autosave.window-seconds:60}")
    private long autosaveWindowSeconds;

    public RequestRateLimiter(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    public void auth(String clientKey) {
        requireAllowed("auth:" + normalized(clientKey), authMaxAttempts, authWindowSeconds);
    }

    public void session(Long userId) {
        requireAllowed("session:" + userId, sessionMaxAttempts, sessionWindowSeconds);
    }

    public void integrity(Long userId) {
        requireAllowed("integrity:" + userId, integrityMaxAttempts, integrityWindowSeconds);
    }

    public void autosave(Long userId) {
        requireAllowed("autosave:" + userId, autosaveMaxAttempts, autosaveWindowSeconds);
    }

    private void requireAllowed(String key, int maxAttempts, long windowSeconds) {
        if (!rateLimitService.tryConsume(key, maxAttempts, windowSeconds)) {
            throw new RateLimitExceededException();
        }
    }

    private String normalized(String key) {
        return key == null || key.isBlank() ? "unknown" : key.trim().toLowerCase();
    }
}
