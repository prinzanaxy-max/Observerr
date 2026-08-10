package com.backend.observerr.student.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class StudentStatsResponse {

    private final long examsCompleted;
    private final int avgIntegrity;
    private final long verifiedSessions;
    private final long underReview;
}
