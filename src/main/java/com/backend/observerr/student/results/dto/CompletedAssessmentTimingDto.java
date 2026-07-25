package com.backend.observerr.student.results.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class CompletedAssessmentTimingDto {

    private final String type;
    private final String startTime;
    private final String endTime;
    private final String submittedTime;
}
