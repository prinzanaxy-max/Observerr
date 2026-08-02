package com.backend.observerr.exam.dto;

import com.backend.observerr.exam.model.AnswerChoice;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SaveAnswerRequest {
    @NotNull
    private AnswerChoice selectedOption;
}
