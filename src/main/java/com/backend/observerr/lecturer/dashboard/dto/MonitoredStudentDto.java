package com.backend.observerr.lecturer.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class MonitoredStudentDto {

    private final Long studentId;
    private final String studentNumber;
    private final String name;
    private final String initials;
    private final String liveStatus;
    private final String liveStatusLabel;
    private final String riskLevel;
    private final String lastEvent;
    private final String latestSessionId;
    private final int integrityScore;
}
