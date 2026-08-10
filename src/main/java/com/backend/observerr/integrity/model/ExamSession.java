package com.backend.observerr.integrity.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "exam_sessions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamSession {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "exam_id", nullable = false)
    private Long examId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "starting_score", nullable = false)
    private short startingScore;

    @Column(name = "final_score")
    private Short finalScore;

    @Builder.Default
    @Column(name = "total_deductions", nullable = false)
    private int totalDeductions = 0;

    @Builder.Default
    @Column(name = "total_events", nullable = false)
    private int totalEvents = 0;

    @Builder.Default
    @Column(name = "requires_review", nullable = false)
    private boolean requiresReview = false;

    @Builder.Default
    @Column(name = "proctoring_available", nullable = false)
    private boolean proctoringAvailable = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExamSessionStatus status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
