package com.backend.observerr.student.exams.dto;

import com.backend.observerr.exam.dto.ExamSecurityDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class StudentExamDto {

    private final Long id;
    private final String title;
    private final String courseCode;
    private final String courseName;
    private final String courseLabel;
    private final String schedule;
    private final String status;
    private final String startAt;
    private final String endAt;
    private final int durationMinutes;
    private final ExamSecurityDto security;
    private final boolean canTake;
    /** True when the student already has a completed session or graded result. */
    private final boolean attempted;
    private final boolean allowRetake;
}
