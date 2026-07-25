package com.backend.observerr.exam.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "exams")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Exam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(name = "lecturer_id", nullable = false)
    private Long lecturerId;

    @Column(name = "course_code", length = 50)
    private String courseCode;

    @Column(name = "course_name")
    private String courseName;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Builder.Default
    @Column(name = "webcam_monitoring", nullable = false)
    private boolean webcamMonitoring = true;

    @Builder.Default
    @Column(name = "tab_switch_tracking", nullable = false)
    private boolean tabSwitchTracking = true;

    @Builder.Default
    @Column(name = "block_copy_paste", nullable = false)
    private boolean blockCopyPaste = true;

    @Builder.Default
    @Column(nullable = false)
    private boolean published = false;

    @Builder.Default
    @Column(name = "enrolled_count", nullable = false)
    private int enrolledCount = 0;

    @Column(name = "capacity_count")
    private Integer capacityCount;

    @Builder.Default
    @Column(name = "active_flags_count", nullable = false)
    private int activeFlagsCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExamStatus status;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time")
    private Instant endTime;

    @Builder.Default
    @Column(name = "start_notifications_sent", nullable = false)
    private boolean startNotificationsSent = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
