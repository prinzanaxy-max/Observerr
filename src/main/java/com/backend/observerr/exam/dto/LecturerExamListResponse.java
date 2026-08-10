package com.backend.observerr.exam.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class LecturerExamListResponse {

    private final List<LecturerExamDto> exams;
    private final long totalElements;
}
