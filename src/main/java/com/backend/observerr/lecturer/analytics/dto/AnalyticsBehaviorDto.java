package com.backend.observerr.lecturer.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AnalyticsBehaviorDto {

    private final String behaviorCode;
    private final String label;
    private final int eventCount;
    private final String icon;
    private final String tone;
}
