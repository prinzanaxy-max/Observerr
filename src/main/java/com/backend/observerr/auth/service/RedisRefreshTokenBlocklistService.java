package com.backend.observerr.auth.service;

import com.backend.observerr.config.RedisClientFactory;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RedisRefreshTokenBlocklistService implements RefreshTokenBlocklistService {

    private static final String KEY_PREFIX = "blacklist:refresh:";

    private final RedisClientFactory.ConnectionHandle handle;
    private final StatefulRedisConnection<String, String> connection;

    public RedisRefreshTokenBlocklistService(RedisClientFactory.ConnectionHandle handle) {
        this.handle = handle;
        this.connection = handle.connection();
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

    public void shutdown() {
        handle.closeQuietly();
    }
}
