package com.backend.observerr.lecturer.students.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class ProctoringSessionEventDto {

    private final Long id;
    private final String eventCode;
    private final String time;
    private final String timestamp;
    private final String eventType;
    private final String severity;
    private final String title;
    private final String description;
    private final Integer pointsDeducted;
    private final Integer scoreAfter;
    private final Integer durationMs;
    private final boolean hasSnapshot;
}
