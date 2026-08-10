package com.backend.observerr.exam.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "exam_student_blocks")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamStudentBlock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "exam_id", nullable = false)
    private Long examId;
    @Column(name = "student_id", nullable = false)
    private Long studentId;
    @Column(name = "blocked_by", nullable = false)
    private Long blockedBy;
    private String reason;
    @CreationTimestamp
    @Column(name = "blocked_at")
    private Instant blockedAt;
    @Column(name = "unblocked_at")
    private Instant unblockedAt;
}
