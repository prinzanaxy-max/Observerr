package com.backend.observerr.student.results.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class CompletedAssessmentDto {

    private final Long id;
    private final String courseName;
    private final String courseCode;
    private final String assessmentType;
    private final String category;
    private final String dateTaken;
    private final CompletedAssessmentTimingDto timing;
    private final int integrityScore;
    private final String status;
}
