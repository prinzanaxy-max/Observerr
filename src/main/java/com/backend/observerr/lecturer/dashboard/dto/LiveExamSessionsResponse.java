package com.backend.observerr.lecturer.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class LiveExamSessionsResponse {

    private final Long examId;
    private final LiveSessionStatsDto stats;
    private final List<MonitoredStudentDto> students;
}
