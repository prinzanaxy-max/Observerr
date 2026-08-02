package com.backend.observerr.exam.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ExamResultDetailDto {
    private final ExamResultDto result;
    private final List<QuestionAnalysisDto> analysis;
}
