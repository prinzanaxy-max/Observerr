package com.backend.observerr.integrity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class IntegrityEventsBatchResponse {

    private final int accepted;
    private final int skipped;
}
