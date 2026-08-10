package com.backend.observerr.security;

import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class InMemoryRateLimitService implements RateLimitService {

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public InMemoryRateLimitService() {
        log.warn("Using in-memory rate limiter (set a reachable REDIS_URL for production)");
    }

    @Override
    public boolean tryConsume(String key, int maxAttempts, long windowSeconds) {
        long now = System.currentTimeMillis();
        purgeExpired(now);
        Window window = windows.computeIfAbsent(key, ignored -> new Window(now + (windowSeconds * 1000L)));
        if (now > window.expiresAtMs) {
            window = new Window(now + (windowSeconds * 1000L));
            windows.put(key, window);
        }
        int count = window.count.incrementAndGet();
        return count <= maxAttempts;
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

    public void shutdown() {
        windows.clear();
    }

    private static final class Window {
        private final AtomicInteger count = new AtomicInteger();
        private final long expiresAtMs;

        private Window(long expiresAtMs) {
            this.expiresAtMs = expiresAtMs;
        }
    }
}
