package com.backend.observerr.lecturer.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class AnalyticsTrendsDto {

    private final String title;
    private final String subtitle;
    private final String granularity;
    private final List<AnalyticsTrendPointDto> points;
}
