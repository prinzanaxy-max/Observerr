package com.backend.observerr.auth.controller;

import com.backend.observerr.auth.dto.AuthResponse;
import com.backend.observerr.auth.dto.LoginRequest;
import com.backend.observerr.auth.dto.LogoutRequest;
import com.backend.observerr.auth.dto.LogoutResponse;
import com.backend.observerr.auth.dto.RegisterRequest;
import com.backend.observerr.auth.model.User;
import com.backend.observerr.auth.service.AuthCookieService;
import com.backend.observerr.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthCookieService authCookieService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @RequestBody @Valid RegisterRequest request,
            HttpServletResponse response) {
        AuthResponse authResponse = authService.register(request);
        authService.attachAuthCookies(response, authResponse);
        return ResponseEntity.status(HttpStatus.CREATED).body(authResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @RequestBody @Valid LoginRequest request,
            HttpServletResponse response) {
        AuthResponse authResponse = authService.login(request);
        authService.attachAuthCookies(response, authResponse);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @RequestHeader(value = "Authorization", required = false) String bearerToken,
            HttpServletRequest request,
            HttpServletResponse response) {
        String refreshToken = resolveRefreshToken(bearerToken, request);
        AuthResponse authResponse = authService.refreshToken(refreshToken);
        authService.attachAuthCookies(response, authResponse);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(
            @AuthenticationPrincipal User user,
            @RequestBody(required = false) LogoutRequest body,
            HttpServletRequest request,
            HttpServletResponse response) {
        boolean allDevices = body != null && Boolean.TRUE.equals(body.getAllDevices());
        String refreshToken = resolveRefreshToken(null, request);
        LogoutResponse logoutResponse = authService.logout(user, refreshToken, allDevices, response);
        return ResponseEntity.ok(logoutResponse);
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "institutionalId", user.getInstitutionalId(),
                "email", user.getEmail(),
                "role", user.getRole().name(),
                "createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : ""
        ));
    }

    private String resolveRefreshToken(String bearerToken, HttpServletRequest request) {
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        if (bearerToken != null && !bearerToken.isBlank()) {
            return bearerToken;
        }
        return authCookieService.extractRefreshToken(request);
    }
}
