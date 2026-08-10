package com.backend.observerr.exam.dto;

import com.backend.observerr.exam.model.AnswerChoice;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class ExamQuestionRequest {

    @NotBlank
    @JsonAlias("prompt")
    private String text;

    @NotNull
    private Map<AnswerChoice, String> options;

    @NotNull
    private AnswerChoice correctAnswer;

    @Min(1)
    private int points = 1;
}
