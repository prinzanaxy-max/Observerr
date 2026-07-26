package com.backend.observerr.student.exams.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class StudentExamListResponse {

    private final List<StudentExamDto> exams;
    private final int totalElements;
}
