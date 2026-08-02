package com.backend.observerr.lecturer.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ProctoringFeedDto {

    private final String sessionId;
    private final String participantIdentity;
    private final String roomName;
    private final Long studentId;
    private final String studentName;
    private final String initials;
    private final int integrityScore;
    private final String riskLevel;
    private final String liveStatusLabel;
    private final String lastEvent;
    private final String snapshotUrl;
}
