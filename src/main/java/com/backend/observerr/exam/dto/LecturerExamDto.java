package com.backend.observerr.exam.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class LecturerExamDto {

    private final Long id;
    private final String title;
    private final String courseCode;
    private final String courseName;
    private final String courseLabel;
    private final String term;
    private final String schedule;
    private final String status;
    private final String enrollment;
    private final int enrolledCount;
    private final Integer capacityCount;
    private final int activeFlagsCount;
    private final String startAt;
    private final int durationMinutes;
    private final ExamSecurityDto security;
    private final ExamDetailBadgeDto detail;
}
