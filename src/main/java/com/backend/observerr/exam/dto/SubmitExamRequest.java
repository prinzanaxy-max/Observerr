package com.backend.observerr.exam.dto;

import com.backend.observerr.integrity.dto.CompleteExamSessionRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class SubmitExamRequest {
    @Valid
    @NotNull
    private List<@Valid SubmitAnswerDto> answers = new ArrayList<>();

    @Valid
    @NotNull
    private CompleteExamSessionRequest completion;
}
