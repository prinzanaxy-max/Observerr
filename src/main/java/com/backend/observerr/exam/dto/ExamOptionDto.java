package com.backend.observerr.exam.dto;

import com.backend.observerr.exam.model.AnswerChoice;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ExamOptionDto {
    private final AnswerChoice key;
    private final String text;
}
