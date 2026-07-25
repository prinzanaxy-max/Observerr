package com.backend.observerr.lecturer.students.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "proctoring_session_events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProctoringSessionEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "event_time", nullable = false)
    private java.time.LocalTime eventTime;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(nullable = false, length = 20)
    private String severity;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "points_deducted")
    private Short pointsDeducted;

    @Column(name = "has_snapshot", nullable = false)
    private boolean hasSnapshot;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
