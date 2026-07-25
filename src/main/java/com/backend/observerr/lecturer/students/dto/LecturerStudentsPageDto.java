package com.backend.observerr.lecturer.students.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class LecturerStudentsPageDto {

    private final List<LecturerStudentDto> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;
    private final int from;
    private final int to;
    private final List<String> availableCourses;
}
