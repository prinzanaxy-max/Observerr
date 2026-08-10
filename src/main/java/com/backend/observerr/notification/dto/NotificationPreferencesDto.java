package com.backend.observerr.notification.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferencesDto {
    private boolean examEvents;
    private boolean integrityAlerts;
    private boolean resultUpdates;
    private boolean systemUpdates;
}
