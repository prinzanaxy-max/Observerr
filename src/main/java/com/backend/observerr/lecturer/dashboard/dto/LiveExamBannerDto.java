package com.backend.observerr.lecturer.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class LiveExamBannerDto {

    private final Long examId;
    private final String title;
    private final String courseCode;
    private final String status;
    private final long remainingSeconds;
    private final int activeStudents;
    private final int highRiskCount;
    private final double avgIntegrityScore;
    private final String liveMonitoringPath;
}
