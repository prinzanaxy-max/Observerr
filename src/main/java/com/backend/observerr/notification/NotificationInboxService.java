package com.backend.observerr.notification;

import com.backend.observerr.notification.dto.*;
import com.backend.observerr.notification.model.NotificationPreference;
import com.backend.observerr.notification.model.UserNotification;
import com.backend.observerr.notification.repository.NotificationPreferenceRepository;
import com.backend.observerr.notification.repository.UserNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class NotificationInboxService {
    private static final Set<String> CATEGORIES = Set.of("EXAM", "INTEGRITY", "RESULT", "SYSTEM");

    private final UserNotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;

    @Transactional(readOnly = true)
    public NotificationPageDto list(
            Long userId, int page, int size, String category, boolean unreadOnly) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(100, Math.max(1, size));
        String normalized = normalizeCategory(category);
        var pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<UserNotification> result;
        if (normalized == null) {
            result = unreadOnly
                    ? notificationRepository.findByUserIdAndReadAtIsNull(userId, pageable)
                    : notificationRepository.findByUserId(userId, pageable);
        } else {
            result = unreadOnly
                    ? notificationRepository.findByUserIdAndCategoryAndReadAtIsNull(userId, normalized, pageable)
                    : notificationRepository.findByUserIdAndCategory(userId, normalized, pageable);
        }
        return NotificationPageDto.builder()
                .content(result.getContent().stream().map(this::toDto).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .unreadCount(notificationRepository.countByUserIdAndReadAtIsNull(userId))
                .build();
    }

    @Transactional
    public void markRead(Long userId, Long notificationId) {
        UserNotification notification = owned(userId, notificationId);
        if (notification.getReadAt() == null) {
            notification.setReadAt(Instant.now());
        }
    }

    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllRead(userId, Instant.now());
    }

    @Transactional
    public void delete(Long userId, Long notificationId) {
        notificationRepository.delete(owned(userId, notificationId));
    }

    @Transactional
    public int deleteAll(Long userId) {
        return notificationRepository.deleteByUserId(userId);
    }

    @Transactional(readOnly = true)
    public NotificationPreferencesDto preferences(Long userId) {
        return toDto(preferenceRepository.findById(userId)
                .orElseGet(() -> defaults(userId)));
    }

    @Transactional
    public NotificationPreferencesDto updatePreferences(Long userId, NotificationPreferencesDto request) {
        NotificationPreference preference = preferenceRepository.findById(userId)
                .orElseGet(() -> defaults(userId));
        preference.setExamEvents(request.isExamEvents());
        preference.setIntegrityAlerts(request.isIntegrityAlerts());
        preference.setResultUpdates(request.isResultUpdates());
        preference.setSystemUpdates(request.isSystemUpdates());
        return toDto(preferenceRepository.save(preference));
    }

    public boolean permits(Long userId, String category) {
        NotificationPreference p = preferenceRepository.findById(userId).orElseGet(() -> defaults(userId));
        return switch (category) {
            case "EXAM" -> p.isExamEvents();
            case "INTEGRITY" -> p.isIntegrityAlerts();
            case "RESULT" -> p.isResultUpdates();
            default -> p.isSystemUpdates();
        };
    }

    @Transactional
    public UserNotification create(
            Long userId, String category, String title, String message, String deepLink, String deduplicationKey) {
        if (!permits(userId, category)) {
            return null;
        }
        if (deduplicationKey != null
                && notificationRepository.findByUserIdAndDeduplicationKey(userId, deduplicationKey).isPresent()) {
            return null;
        }
        return notificationRepository.save(UserNotification.builder()
                .userId(userId)
                .category(category)
                .title(title)
                .message(message)
                .deepLink(deepLink)
                .deduplicationKey(deduplicationKey)
                .build());
    }

    private UserNotification owned(Long userId, Long id) {
        return notificationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
    }

    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) return null;
        String normalized = category.trim().toUpperCase();
        if (!CATEGORIES.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported notification category");
        }
        return normalized;
    }

    private NotificationPreference defaults(Long userId) {
        return NotificationPreference.builder().userId(userId).build();
    }

    private NotificationItemDto toDto(UserNotification n) {
        return NotificationItemDto.builder()
                .id(n.getId()).category(n.getCategory()).title(n.getTitle()).message(n.getMessage())
                .read(n.getReadAt() != null)
                .createdAt(n.getCreatedAt() == null ? Instant.now().toString() : n.getCreatedAt().toString())
                .deepLink(n.getDeepLink()).deduplicationKey(n.getDeduplicationKey()).build();
    }

    private NotificationPreferencesDto toDto(NotificationPreference p) {
        return NotificationPreferencesDto.builder()
                .examEvents(p.isExamEvents()).integrityAlerts(p.isIntegrityAlerts())
                .resultUpdates(p.isResultUpdates()).systemUpdates(p.isSystemUpdates()).build();
    }
}
