package com.backend.observerr.exam.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "exam_answers", uniqueConstraints = @UniqueConstraint(
        name = "uk_exam_answers_session_question", columnNames = {"session_id", "question_id"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "selected_option_key", nullable = false, length = 1)
    private AnswerChoice selectedOptionKey;

    @Builder.Default
    @Column(nullable = false)
    private boolean submitted = false;

    @Column(name = "saved_at", nullable = false)
    private Instant savedAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;
}
