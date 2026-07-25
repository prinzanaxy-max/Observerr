package com.backend.observerr.exam.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateExamRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String course;

    @NotBlank
    private String startAt;

    @NotNull
    @Min(15)
    private Integer durationMinutes;

    @Valid
    @NotNull
    private ExamSecurityDto security;

    private boolean publish = true;
}
