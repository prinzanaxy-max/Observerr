package com.backend.observerr.exam.dto;

import com.backend.observerr.exam.model.AnswerChoice;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QuestionAnalysisDto {
    private final Long questionId;
    private final String question;
    private final AnswerChoice selectedAnswer;
    private final AnswerChoice correctAnswer;
    private final boolean correct;
    private final int pointsEarned;
    private final int pointsPossible;
}
