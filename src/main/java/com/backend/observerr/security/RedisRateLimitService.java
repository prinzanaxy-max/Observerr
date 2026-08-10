package com.backend.observerr.security;

import com.backend.observerr.config.RedisClientFactory;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RedisRateLimitService implements RateLimitService {

    private final RedisClientFactory.ConnectionHandle handle;
    private final StatefulRedisConnection<String, String> connection;

    public RedisRateLimitService(RedisClientFactory.ConnectionHandle handle) {
        this.handle = handle;
        this.connection = handle.connection();
    }

    @Override
    public boolean tryConsume(String key, int maxAttempts, long windowSeconds) {
        RedisCommands<String, String> commands = connection.sync();
        String redisKey = "rate:" + key;
        Long count = commands.incr(redisKey);
        if (count != null && count == 1L) {
            commands.expire(redisKey, windowSeconds);
        }
        return count != null && count <= maxAttempts;
    }

    @Override
    public boolean isDistributed() {
        return true;
    }

    @Override
    public boolean isReady() {
        try {
            return "PONG".equalsIgnoreCase(connection.sync().ping());
        } catch (RuntimeException ex) {
            log.error("Redis readiness check failed: {}", ex.getMessage());
            return false;
        }
    }

    public void shutdown() {
        handle.closeQuietly();
    }
}
