package com.backend.observerr.integrity.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class IntegrityEventsBatchRequest {

    @NotEmpty
    @Valid
    private List<IntegrityEventIngestDto> events;
}
