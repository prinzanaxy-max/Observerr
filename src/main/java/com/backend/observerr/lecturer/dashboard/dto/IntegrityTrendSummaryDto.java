package com.backend.observerr.lecturer.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class IntegrityTrendSummaryDto {

    private final String changeLabel;
    private final String changeDirection;
    private final List<Integer> points;
}
