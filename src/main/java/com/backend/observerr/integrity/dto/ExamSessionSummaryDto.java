package com.backend.observerr.integrity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExamSessionSummaryDto {

    @NotBlank
    private String sessionId;

    @NotNull
    private Long examId;

    @NotBlank
    private String startedAt;

    @NotBlank
    private String endedAt;

    private int startingScore;
    private int finalScore;
    private int totalEvents;
    private int totalDeductions;
    private boolean requiresReview;
    private boolean proctoringAvailable;
}
