package com.backend.observerr.notification.controller;

import com.backend.observerr.auth.model.User;
import com.backend.observerr.notification.dto.RegisterDeviceTokenRequest;
import com.backend.observerr.notification.service.DeviceTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceTokenController {

    private final DeviceTokenService deviceTokenService;

    @PostMapping("/token")
    public ResponseEntity<Map<String, Object>> registerToken(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid RegisterDeviceTokenRequest request) {
        deviceTokenService.registerToken(user, request);
        return ResponseEntity.ok(Map.of("success", true, "message", "Device token registered"));
    }

    @DeleteMapping("/token")
    public ResponseEntity<Void> unregisterToken(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid RegisterDeviceTokenRequest request) {
        deviceTokenService.unregisterToken(user, request);
        return ResponseEntity.noContent().build();
    }
}
