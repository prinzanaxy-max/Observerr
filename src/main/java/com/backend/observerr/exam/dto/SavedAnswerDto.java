package com.backend.observerr.exam.dto;

import com.backend.observerr.exam.model.AnswerChoice;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SavedAnswerDto {
    private final Long questionId;
    private final AnswerChoice selectedOption;
    private final String savedAt;
    private final boolean submitted;
}
