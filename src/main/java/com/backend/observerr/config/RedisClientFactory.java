package com.backend.observerr.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.api.StatefulRedisConnection;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

/**
 * Builds Lettuce clients with short connect timeouts so a bad REDIS_URL cannot
 * hang Spring Boot startup past Railway healthchecks.
 */
@Slf4j
public final class RedisClientFactory {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration COMMAND_TIMEOUT = Duration.ofSeconds(3);

    private RedisClientFactory() {
    }

    public static ConnectionHandle connect(String redisUrl) {
        RedisURI uri = RedisURI.create(redisUrl);
        uri.setTimeout(COMMAND_TIMEOUT);

        RedisClient client = RedisClient.create(uri);
        client.setOptions(ClientOptions.builder()
                .socketOptions(SocketOptions.builder()
                        .connectTimeout(CONNECT_TIMEOUT)
                        .build())
                .autoReconnect(true)
                .build());

        StatefulRedisConnection<String, String> connection = client.connect();
        connection.setTimeout(COMMAND_TIMEOUT);
        String pong = connection.sync().ping();
        if (!"PONG".equalsIgnoreCase(pong)) {
            connection.close();
            client.shutdown();
            throw new IllegalStateException("Redis ping returned unexpected response: " + pong);
        }
        return new ConnectionHandle(client, connection);
    }

    public record ConnectionHandle(RedisClient client, StatefulRedisConnection<String, String> connection) {
        public void closeQuietly() {
            try {
                connection.close();
            } catch (RuntimeException ex) {
                log.debug("Error closing Redis connection: {}", ex.getMessage());
            }
            try {
                client.shutdown();
            } catch (RuntimeException ex) {
                log.debug("Error shutting down Redis client: {}", ex.getMessage());
            }
        }
    }
}
