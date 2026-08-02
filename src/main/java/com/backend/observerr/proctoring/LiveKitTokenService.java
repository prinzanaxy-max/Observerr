package com.backend.observerr.proctoring;

import com.backend.observerr.proctoring.dto.LiveKitTokenResponse;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Date;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LiveKitTokenService {

    private static final long MIN_TTL_SECONDS = 30;
    private static final long MAX_TTL_SECONDS = 900;

    private final LiveKitProperties properties;

    public LiveKitTokenResponse createPublisherToken(Long examId, String sessionId) {
        return createToken(examId, sessionId, true, false);
    }

    public LiveKitTokenResponse createSubscriberToken(Long examId, Long lecturerId) {
        return createToken(examId, "lecturer-" + lecturerId, false, true);
    }

    public String roomName(Long examId) {
        return "exam-" + examId;
    }

    private LiveKitTokenResponse createToken(
            Long examId,
            String participantIdentity,
            boolean canPublish,
            boolean canSubscribe) {
        validateConfiguration();

        Instant issuedAt = Instant.now();
        long ttl = Math.max(MIN_TTL_SECONDS, Math.min(MAX_TTL_SECONDS, properties.getTokenTtlSeconds()));
        Instant expiresAt = issuedAt.plusSeconds(ttl);
        String roomName = roomName(examId);
        SecretKey key = Keys.hmacShaKeyFor(properties.getApiSecret().getBytes(StandardCharsets.UTF_8));

        Map<String, Object> videoGrant = new HashMap<>();
        videoGrant.put("roomJoin", true);
        videoGrant.put("room", roomName);
        videoGrant.put("canPublish", canPublish);
        videoGrant.put("canSubscribe", canSubscribe);
        videoGrant.put("canPublishData", false);
        if (canPublish) {
            videoGrant.put("canPublishSources", List.of("camera", "microphone"));
        }

        String token = Jwts.builder()
                .issuer(properties.getApiKey())
                .subject(participantIdentity)
                .issuedAt(Date.from(issuedAt))
                .notBefore(Date.from(issuedAt.minusSeconds(5)))
                .expiration(Date.from(expiresAt))
                .claim("video", videoGrant)
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        return LiveKitTokenResponse.builder()
                .url(properties.getUrl())
                .token(token)
                .roomName(roomName)
                .participantIdentity(participantIdentity)
                .expiresAt(expiresAt.toString())
                .build();
    }

    private void validateConfiguration() {
        if (isBlank(properties.getUrl())
                || isBlank(properties.getApiKey())
                || isBlank(properties.getApiSecret())
                || properties.getApiSecret().getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Live proctoring is not configured");
        }
        if (!properties.getUrl().startsWith("wss://") && !properties.getUrl().startsWith("ws://")) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Live proctoring URL is invalid");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
