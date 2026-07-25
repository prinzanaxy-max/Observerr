package com.backend.observerr.student.results.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "student_completed_assessments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentCompletedAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "course_name", nullable = false)
    private String courseName;

    @Column(name = "course_code", nullable = false, length = 50)
    private String courseCode;

    @Column(name = "assessment_type", nullable = false, length = 100)
    private String assessmentType;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(name = "taken_date", nullable = false)
    private LocalDate takenDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "timing_type", nullable = false, length = 20)
    private AssessmentTimingType timingType;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "submitted_time")
    private LocalTime submittedTime;

    @Column(name = "integrity_score", nullable = false)
    private short integrityScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssessmentResultStatus status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
