package com.backend.observerr.integrity.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CompleteExamSessionRequest {

    @NotNull
    @Valid
    private ExamSessionSummaryDto summary;

    private List<@Valid IntegrityEventIngestDto> events;
}
