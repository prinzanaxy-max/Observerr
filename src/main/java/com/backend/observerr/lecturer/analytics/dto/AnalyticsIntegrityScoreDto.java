package com.backend.observerr.lecturer.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
public class AnalyticsIntegrityScoreDto {

    private final BigDecimal value;
    private final BigDecimal changePercent;
    private final String changeDirection;
    private final String changeLabel;
}
