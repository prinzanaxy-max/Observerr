package com.backend.observerr.lecturer.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AnalyticsMostCommonFlagDto {

    private final String label;
    private final int sharePercent;
    private final String icon;
}
