package com.backend.observerr.exam.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ExamResultsPageDto {
    private final List<ExamResultDto> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;
}
