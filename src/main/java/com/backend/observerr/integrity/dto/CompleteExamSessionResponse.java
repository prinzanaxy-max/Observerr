package com.backend.observerr.integrity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class CompleteExamSessionResponse {

    private final String sessionId;
    private final int finalScore;
    private final boolean requiresReview;
    private final String status;
}
