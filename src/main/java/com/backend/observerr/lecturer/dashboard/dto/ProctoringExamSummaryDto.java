package com.backend.observerr.lecturer.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ProctoringExamSummaryDto {

    private final Long examId;
    private final String title;
    private final String courseLabel;
    private final int activeFeeds;
    private final int totalStudents;
}
