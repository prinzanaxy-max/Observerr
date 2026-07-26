package com.backend.observerr.lecturer.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class LecturerAnalyticsOverviewResponse {

    private final String period;
    private final AnalyticsMetricCardDto totalExamsMonitored;
    private final AnalyticsMetricCardDto totalFlaggedEvents;
    private final AnalyticsIntegrityScoreDto avgIntegrityScore;
    private final AnalyticsMostCommonFlagDto mostCommonFlag;
    private final AnalyticsTrendsDto trends;
    private final List<AnalyticsBehaviorDto> topBehaviors;
}
