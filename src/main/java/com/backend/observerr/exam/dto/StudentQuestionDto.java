package com.backend.observerr.exam.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class StudentQuestionDto {
    private final Long id;
    private final String text;
    private final int order;
    private final int points;
    private final List<ExamOptionDto> options;
}
