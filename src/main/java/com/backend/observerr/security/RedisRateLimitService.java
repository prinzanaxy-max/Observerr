package com.backend.observerr.security;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnExpression("!'${spring.data.redis.url:}'.trim().isEmpty()")
public class RedisRateLimitService implements RateLimitService {

    private final RedisClient redisClient;
    private final StatefulRedisConnection<String, String> connection;

    public RedisRateLimitService(@Value("${spring.data.redis.url}") String redisUrl) {
        this.redisClient = RedisClient.create(redisUrl);
        this.connection = redisClient.connect();
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

    @PreDestroy
    public void shutdown() {
        connection.close();
        redisClient.shutdown();
    }
}
