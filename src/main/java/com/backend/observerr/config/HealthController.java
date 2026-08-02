package com.backend.observerr.config;

import com.backend.observerr.security.RateLimitService;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;

@RestController
public class HealthController {
    private final DataSource dataSource;
    private final RateLimitService rateLimitService;

    @Value("${spring.data.redis.url:}")
    private String redisUrl;

    @Value("${firebase.service-account-json:}")
    private String firebaseCredentials;

    public HealthController(DataSource dataSource, RateLimitService rateLimitService) {
        this.dataSource = dataSource;
        this.rateLimitService = rateLimitService;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("app", "Observerr Backend");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("version", "1.0.0");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> readiness() {
        Map<String, Object> checks = new HashMap<>();
        boolean databaseUp;
        try (var connection = dataSource.getConnection()) {
            databaseUp = connection.isValid(2);
        } catch (Exception ex) {
            databaseUp = false;
        }
        checks.put("database", databaseUp ? "UP" : "DOWN");
        boolean redisConfigured = redisUrl != null && !redisUrl.isBlank();
        boolean redisUp = !redisConfigured || (rateLimitService.isDistributed() && rateLimitService.isReady());
        checks.put("redis", !redisConfigured ? "OPTIONAL_DISABLED" : redisUp ? "UP" : "DOWN");
        checks.put("fcm", firebaseCredentials == null || firebaseCredentials.isBlank()
                ? "OPTIONAL_DISABLED" : "CONFIGURED");
        Map<String, Object> response = new HashMap<>();
        boolean ready = databaseUp && redisUp;
        response.put("status", ready ? "UP" : "DOWN");
        response.put("checks", checks);
        response.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.status(ready ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
                .body(response);
    }
}
