package com.backend.observerr.lecturer.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class LiveSessionStatsDto {

    private final int active;
    private final int total;
    private final int highRisk;
    private final int warnings;
    private final int networkStability;
}
