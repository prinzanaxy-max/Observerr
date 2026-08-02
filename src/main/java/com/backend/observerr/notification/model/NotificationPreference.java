package com.backend.observerr.notification.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "notification_preferences")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreference {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @Builder.Default
    @Column(name = "exam_events", nullable = false)
    private boolean examEvents = true;

    @Builder.Default
    @Column(name = "integrity_alerts", nullable = false)
    private boolean integrityAlerts = true;

    @Builder.Default
    @Column(name = "result_updates", nullable = false)
    private boolean resultUpdates = true;

    @Builder.Default
    @Column(name = "system_updates", nullable = false)
    private boolean systemUpdates = true;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
