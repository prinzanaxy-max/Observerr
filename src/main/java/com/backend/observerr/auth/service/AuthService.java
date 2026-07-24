package com.backend.observerr.auth.service;

import com.backend.observerr.auth.dto.AuthResponse;
import com.backend.observerr.auth.dto.LoginRequest;
import com.backend.observerr.auth.dto.LogoutResponse;
import com.backend.observerr.auth.dto.RegisterRequest;
import com.backend.observerr.auth.model.User;
import com.backend.observerr.auth.model.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenBlocklistService refreshTokenBlocklistService;
    private final AuthCookieService authCookieService;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .build();

        userRepository.save(user);

        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Failed login attempt for email: {}", request.getEmail());
                    return new RuntimeException("Invalid credentials");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Failed login attempt — wrong password for email: {}", request.getEmail());
            throw new RuntimeException("Invalid credentials");
        }

        return buildAuthResponse(user);
    }

    public AuthResponse refreshToken(String refreshToken) {
        User user = resolveUserFromRefreshToken(refreshToken);

        // All refresh token verification must check the Redis blocklist before trusting the token.
        assertRefreshTokenNotBlocklisted(refreshToken);
        assertRefreshTokenValid(refreshToken, user);

        blocklistRefreshToken(refreshToken);

        return buildAuthResponse(user);
    }

    @Transactional
    public LogoutResponse logout(User user, String refreshToken, boolean allDevices, HttpServletResponse response) {
        authCookieService.clearAuthCookies(response);

        if (refreshToken != null && !refreshToken.isBlank()) {
            try {
                blocklistRefreshToken(refreshToken);
            } catch (Exception ex) {
                log.debug("Could not blocklist refresh token during logout: {}", ex.getMessage());
            }
        }

        if (allDevices && user != null) {
            user.setTokenVersion(user.getTokenVersion() + 1);
            userRepository.save(user);
        }

        Long userId = user != null ? user.getId() : null;
        log.info("User logout userId={} timestamp={} allDevices={}", userId, Instant.now(), allDevices);

        return LogoutResponse.builder()
                .success(true)
                .message("Logged out successfully")
                .build();
    }

    public void attachAuthCookies(HttpServletResponse response, AuthResponse authResponse) {
        authCookieService.setAuthCookies(response, authResponse.getAccessToken(), authResponse.getRefreshToken());
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .role(user.getRole().name())
                .fullName(user.getFullName())
                .expiresIn(jwtService.getExpiration())
                .build();
    }

    private User resolveUserFromRefreshToken(String refreshToken) {
        String email = jwtService.extractEmail(refreshToken);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));
    }

    private void assertRefreshTokenNotBlocklisted(String refreshToken) {
        String jti = jwtService.extractJti(refreshToken);
        if (refreshTokenBlocklistService.isBlocked(jti)) {
            throw new RuntimeException("Invalid credentials");
        }
    }

    private void assertRefreshTokenValid(String refreshToken, User user) {
        if (!jwtService.isRefreshTokenValid(refreshToken, user)) {
            throw new RuntimeException("Invalid credentials");
        }
    }

    private void blocklistRefreshToken(String refreshToken) {
        String jti = jwtService.extractJti(refreshToken);
        long ttlSeconds = jwtService.getRemainingTtlSeconds(refreshToken);
        refreshTokenBlocklistService.blocklist(jti, ttlSeconds);
    }
}
