package com.backend.observerr.notification.controller;

import com.backend.observerr.auth.model.User;
import com.backend.observerr.notification.NotificationInboxService;
import com.backend.observerr.notification.dto.NotificationPageDto;
import com.backend.observerr.notification.dto.NotificationPreferencesDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationInboxService inboxService;

    @GetMapping
    public NotificationPageDto list(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "false") boolean unreadOnly) {
        return inboxService.list(user.getId(), page, size, category, unreadOnly);
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@AuthenticationPrincipal User user, @PathVariable Long id) {
        inboxService.markRead(user.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal User user) {
        inboxService.markAllRead(user.getId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal User user, @PathVariable Long id) {
        inboxService.delete(user.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAll(@AuthenticationPrincipal User user) {
        inboxService.deleteAll(user.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/preferences")
    public NotificationPreferencesDto preferences(@AuthenticationPrincipal User user) {
        return inboxService.preferences(user.getId());
    }

    @PutMapping("/preferences")
    public NotificationPreferencesDto updatePreferences(
            @AuthenticationPrincipal User user,
            @RequestBody NotificationPreferencesDto preferences) {
        return inboxService.updatePreferences(user.getId(), preferences);
    }
}
