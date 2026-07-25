package com.backend.observerr.exam.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ExamDetailBadgeDto {

    private final String type;
    private final String label;
    private final String icon;
    private final String tone;
}
