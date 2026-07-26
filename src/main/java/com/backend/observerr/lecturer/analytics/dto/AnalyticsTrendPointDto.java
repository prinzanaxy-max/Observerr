package com.backend.observerr.lecturer.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AnalyticsTrendPointDto {

    private final String label;
    private final int monitoredSessions;
    private final int flaggedEvents;
    private final boolean alert;
}
