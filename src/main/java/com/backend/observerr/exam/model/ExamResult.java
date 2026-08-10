package com.backend.observerr.exam.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "exam_results", uniqueConstraints = @UniqueConstraint(
        name = "uk_exam_results_session", columnNames = "session_id"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exam_id", nullable = false)
    private Long examId;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "lecturer_id", nullable = false)
    private Long lecturerId;

    @Column(name = "academic_score", nullable = false)
    private int academicScore;

    @Column(name = "max_score", nullable = false)
    private int maxScore;

    @Column(name = "integrity_score", nullable = false)
    private short integrityScore;

    @Column(name = "requires_review", nullable = false)
    private boolean requiresReview;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "release_status", nullable = false, length = 20)
    private ExamResultStatus releaseStatus = ExamResultStatus.PENDING;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @Column(name = "released_at")
    private Instant releasedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
