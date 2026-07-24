package com.backend.observerr.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@ConditionalOnMissingBean(RefreshTokenBlocklistService.class)
public class InMemoryRefreshTokenBlocklistService implements RefreshTokenBlocklistService {

    private final Map<String, Long> blocklist = new ConcurrentHashMap<>();

    @Override
    public void blocklist(String jti, long ttlSeconds) {
        if (jti == null || jti.isBlank() || ttlSeconds <= 0) {
            return;
        }
        blocklist.put(jti, System.currentTimeMillis() + (ttlSeconds * 1000L));
        log.warn("Using in-memory refresh token blocklist (set REDIS_URL for production)");
    }

    @Override
    public boolean isBlocked(String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }
        purgeExpiredEntries();
        Long expiresAt = blocklist.get(jti);
        if (expiresAt == null) {
            return false;
        }
        if (expiresAt <= System.currentTimeMillis()) {
            blocklist.remove(jti);
            return false;
        }
        return true;
    }

    private void purgeExpiredEntries() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Long>> iterator = blocklist.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            if (entry.getValue() <= now) {
                iterator.remove();
            }
        }
    }
}
