package com.backend.observerr.security;

public interface RateLimitService {

    /**
     * @return true if the request is allowed, false if the limit is exceeded
     */
    boolean tryConsume(String key, int maxAttempts, long windowSeconds);

    default boolean isDistributed() {
        return false;
    }

    default boolean isReady() {
        return true;
    }
}
