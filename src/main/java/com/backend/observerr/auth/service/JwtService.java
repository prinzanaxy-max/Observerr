package com.backend.observerr.auth.service;

import com.backend.observerr.auth.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;

@Slf4j
@Service
public class JwtService {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_FULL_NAME = "fullName";
    private static final String CLAIM_JTI = "jti";
    private static final String CLAIM_TOKEN_VERSION = "tokenVersion";
    private static final String CLAIM_TYPE = "type";
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    @Value("${jwt.issuer}")
    private String issuer;

    public String generateAccessToken(User user) {
        return Jwts.builder()
                .subject(user.getEmail())
                .claim(CLAIM_ROLE, user.getRole().name())
                .claim(CLAIM_FULL_NAME, user.getFullName())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .issuer(issuer)
                .signWith(getSigningKey())
                .compact();
    }

    public String generateRefreshToken(User user) {
        String jti = UUID.randomUUID().toString();
        return Jwts.builder()
                .subject(user.getEmail())
                .id(jti)
                .claim(CLAIM_JTI, jti)
                .claim(CLAIM_TOKEN_VERSION, user.getTokenVersion())
                .claim(CLAIM_TYPE, TOKEN_TYPE_REFRESH)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpiration))
                .issuer(issuer)
                .signWith(getSigningKey())
                .compact();
    }

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get(CLAIM_ROLE, String.class));
    }

    public String extractJti(String token) {
        String jti = extractClaim(token, claims -> claims.get(CLAIM_JTI, String.class));
        if (jti != null && !jti.isBlank()) {
            return jti;
        }
        return extractClaim(token, Claims::getId);
    }

    public int extractTokenVersion(String token) {
        Integer version = extractClaim(token, claims -> claims.get(CLAIM_TOKEN_VERSION, Integer.class));
        return version != null ? version : 0;
    }

    public boolean isRefreshToken(String token) {
        String type = extractClaim(token, claims -> claims.get(CLAIM_TYPE, String.class));
        return TOKEN_TYPE_REFRESH.equals(type);
    }

    public long getRemainingTtlSeconds(String token) {
        Date expirationDate = extractExpiration(token);
        long remainingMs = expirationDate.getTime() - System.currentTimeMillis();
        return Math.max(remainingMs / 1000, 0);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String email = extractEmail(token);
        return email.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    public boolean isRefreshTokenValid(String token, User user) {
        return isTokenValid(token, user)
                && isRefreshToken(token)
                && extractTokenVersion(token) == user.getTokenVersion();
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public long getExpiration() {
        return expiration;
    }
}
