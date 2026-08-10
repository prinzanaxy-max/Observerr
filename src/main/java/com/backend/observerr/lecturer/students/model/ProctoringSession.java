package com.backend.observerr.lecturer.students.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "proctoring_sessions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProctoringSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "lecturer_id", nullable = false)
    private Long lecturerId;

    @Column(name = "course_code", nullable = false, length = 50)
    private String courseCode;

    @Column(name = "course_name", nullable = false)
    private String courseName;

    @Column(name = "assessment_title", nullable = false)
    private String assessmentTitle;

    @Column(name = "integrity_score", nullable = false)
    private short integrityScore;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "total_flags", nullable = false)
    private int totalFlags;

    @Column(name = "device_flags", nullable = false)
    private int deviceFlags;

    @Column(name = "absence_flags", nullable = false)
    private int absenceFlags;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "started_at", nullable = false)
    private LocalTime startedAt;

    @Column(name = "ended_at", nullable = false)
    private LocalTime endedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
