package com.backend.observerr.student.results.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class CompletedAssessmentsSummaryDto {

    private final long examsCompleted;
    private final int avgIntegrity;
    private final long verifiedSessions;
    private final long underReview;
}
