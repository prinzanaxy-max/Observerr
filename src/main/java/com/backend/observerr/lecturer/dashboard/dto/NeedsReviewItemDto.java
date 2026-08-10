package com.backend.observerr.lecturer.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class NeedsReviewItemDto {

    private final Long studentId;
    private final String studentName;
    private final String initials;
    private final String examTitle;
    private final Long examId;
    private final String riskLevel;
    private final int integrityScore;
    private final String latestSessionId;
    private final boolean requiresReview;
}
