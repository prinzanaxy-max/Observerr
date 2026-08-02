package com.backend.observerr.exam.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "exam_question_options", uniqueConstraints = @UniqueConstraint(
        name = "uk_exam_question_options_key", columnNames = {"question_id", "option_key"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamQuestionOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "option_key", nullable = false, length = 1)
    private AnswerChoice optionKey;

    @Column(name = "option_text", nullable = false, columnDefinition = "text")
    private String optionText;

    @Column(name = "correct_answer", nullable = false)
    private boolean correctAnswer;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
