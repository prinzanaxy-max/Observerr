package com.backend.observerr.auth.service;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnProperty(name = "spring.data.redis.url", matchIfMissing = false)
public class RedisRefreshTokenBlocklistService implements RefreshTokenBlocklistService {

    private static final String KEY_PREFIX = "blacklist:refresh:";

    private final RedisClient redisClient;
    private final StatefulRedisConnection<String, String> connection;

    public RedisRefreshTokenBlocklistService(@Value("${spring.data.redis.url}") String redisUrl) {
        this.redisClient = RedisClient.create(redisUrl);
        this.connection = redisClient.connect();
    }

    @Override
    public void blocklist(String jti, long ttlSeconds) {
        if (jti == null || jti.isBlank() || ttlSeconds <= 0) {
            return;
        }
        RedisCommands<String, String> commands = connection.sync();
        commands.setex(KEY_PREFIX + jti, ttlSeconds, "1");
        log.debug("Blocklisted refresh token jti={} for {}s", jti, ttlSeconds);
    }

    @Override
    public boolean isBlocked(String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }
        return "1".equals(connection.sync().get(KEY_PREFIX + jti));
    }

    @PreDestroy
    public void shutdown() {
        connection.close();
        redisClient.shutdown();
    }
}
