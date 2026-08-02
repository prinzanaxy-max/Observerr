package com.backend.observerr.exam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ReplaceExamEnrollmentsRequest {

    @NotNull
    private List<@NotBlank String> studentInstitutionalIds = new ArrayList<>();
}
