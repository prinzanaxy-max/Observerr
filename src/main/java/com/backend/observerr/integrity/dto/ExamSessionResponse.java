package com.backend.observerr.integrity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ExamSessionResponse {

    private final String sessionId;
    private final Long examId;
    private final Long studentId;
    private final String startedAt;
    private final int startingScore;
    private final String status;
}
