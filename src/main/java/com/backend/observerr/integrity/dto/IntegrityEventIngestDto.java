package com.backend.observerr.integrity.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
public class IntegrityEventIngestDto {

    @NotNull
    private UUID clientEventId;

    @NotBlank
    private String eventCode;

    @NotBlank
    private String title;

    private String description;

    @NotBlank
    private String severity;

    private int pointsDeducted;

    private int scoreAfter;

    private boolean requiresReview;

    @NotBlank
    private String timestamp;

    private Integer durationMs;

    private Map<String, Object> metadata;
}
