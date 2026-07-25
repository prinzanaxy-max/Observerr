package com.backend.observerr.student.results.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class CompletedAssessmentsPageDto {

    private final List<CompletedAssessmentDto> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;
    private final String sort;
    private final int from;
    private final int to;
}
