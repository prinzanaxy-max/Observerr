package com.backend.observerr.lecturer.students.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class ProctoringSessionDetailDto {

    private final String sessionId;
    private final Long studentId;
    private final String studentNumber;
    private final String studentName;
    private final String initials;
    private final String assessmentTitle;
    private final String courseCode;
    private final String courseName;
    private final String courseLabel;
    private final int integrityScore;
    private final boolean requiresReview;
    private final String duration;
    private final int totalFlags;
    private final int deviceFlags;
    private final int absenceFlags;
    private final String sessionDate;
    private final List<ProctoringSessionEventDto> events;
}
