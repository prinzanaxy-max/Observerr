package com.backend.observerr.lecturer.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
public class AnalyticsMetricCardDto {

    private final long value;
    private final BigDecimal changePercent;
    private final String changeDirection;
    private final String changeLabel;
}
