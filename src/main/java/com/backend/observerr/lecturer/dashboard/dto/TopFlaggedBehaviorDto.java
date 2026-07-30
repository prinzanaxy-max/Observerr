package com.backend.observerr.lecturer.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class TopFlaggedBehaviorDto {

    private final String behaviorCode;
    private final String label;
    private final int eventCount;
    private final int sharePercent;
    private final String tone;
    private final String icon;
}
