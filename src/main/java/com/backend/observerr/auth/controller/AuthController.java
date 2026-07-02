package com.backend.observerr.auth.controller;

/*
 * ============================================
 * CURL TEST COMMANDS — Observerr Auth API
 * ============================================
 *
 * HEALTH CHECK:
 * curl -X GET https://observerr-production.up.railway.app/health
 *
 * REGISTER (Student):
 * curl -X POST https://observerr-production.up.railway.app/api/auth/register \
 *   -H "Content-Type: application/json" \
 *   -d '{
 *     "fullName": "Kofi Atta",
 *     "email": "kofi@university.edu",
 *     "password": "password123",
 *     "role": "STUDENT"
 *   }'
 *
 * REGISTER (Lecturer):
 * curl -X POST https://observerr-production.up.railway.app/api/auth/register \
 *   -H "Content-Type: application/json" \
 *   -d '{
 *     "fullName": "Dr. Mensah",
 *     "email": "mensah@university.edu",
 *     "password": "password123",
 *     "role": "LECTURER"
 *   }'
 *
 * LOGIN:
 * curl -X POST https://observerr-production.up.railway.app/api/auth/login \
 *   -H "Content-Type: application/json" \
 *   -d '{
 *     "email": "kofi@university.edu",
 *     "password": "password123"
 *   }'
 *
 * GET CURRENT USER (replace TOKEN with actual JWT):
 * curl -X GET https://observerr-production.up.railway.app/api/auth/me \
 *   -H "Authorization: Bearer TOKEN"
 *
 * STUDENT HELLO (replace TOKEN):
 * curl -X GET https://observerr-production.up.railway.app/api/student/hello \
 *   -H "Authorization: Bearer TOKEN"
 *
 * LECTURER HELLO (replace TOKEN):
 * curl -X GET https://observerr-production.up.railway.app/api/lecturer/hello \
 *   -H "Authorization: Bearer TOKEN"
 * ============================================
 */

import com.backend.observerr.auth.dto.AuthResponse;
import com.backend.observerr.auth.dto.LoginRequest;
import com.backend.observerr.auth.dto.RegisterRequest;
import com.backend.observerr.auth.model.User;
import com.backend.observerr.auth.service.AuthService;
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

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody @Valid RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @RequestHeader("Authorization") String bearerToken) {
        String refreshToken = bearerToken.startsWith("Bearer ")
                ? bearerToken.substring(7)
                : bearerToken;
        AuthResponse response = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "fullName", user.getFullName(),
                "role", user.getRole().name(),
                "createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : ""
        ));
    }
}
