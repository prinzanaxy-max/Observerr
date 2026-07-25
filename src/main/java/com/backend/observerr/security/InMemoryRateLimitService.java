package com.backend.observerr.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@ConditionalOnMissingBean(RateLimitService.class)
public class InMemoryRateLimitService implements RateLimitService {

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    @Override
    public boolean tryConsume(String key, int maxAttempts, long windowSeconds) {
        long now = System.currentTimeMillis();
        purgeExpired(now);
        Window window = windows.computeIfAbsent(key, ignored -> new Window(now + (windowSeconds * 1000L)));
        if (now > window.expiresAtMs) {
            window = new Window(now + (windowSeconds * 1000L));
            windows.put(key, window);
        }
        window.count++;
        log.warn("Using in-memory rate limiter (set REDIS_URL for production)");
        return window.count <= maxAttempts;
    }

    private void purgeExpired(long now) {
        Iterator<Map.Entry<String, Window>> iterator = windows.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Window> entry = iterator.next();
            if (now > entry.getValue().expiresAtMs) {
                iterator.remove();
            }
        }
    }

    private static final class Window {
        private int count;
        private final long expiresAtMs;

        private Window(long expiresAtMs) {
            this.expiresAtMs = expiresAtMs;
        }
    }
}
