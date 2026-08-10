package com.backend.observerr.exam.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ExamResultDto {
    private final Long id;
    private final Long examId;
    private final String sessionId;
    private final Long studentId;
    private final String studentName;
    private final String examTitle;
    private final String courseCode;
    private final int academicScore;
    private final int maxScore;
    private final double percentage;
    private final int integrityScore;
    private final boolean requiresReview;
    private final String status;
    private final String submittedAt;
    private final String releasedAt;
}
