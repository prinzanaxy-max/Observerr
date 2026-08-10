package com.backend.observerr.integrity.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StartExamSessionRequest {

    @Min(0)
    @Max(100)
    private int startingScore = 100;
}
