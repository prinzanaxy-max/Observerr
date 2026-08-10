package com.backend.observerr.lecturer.dashboard.dto;

import com.backend.observerr.exam.dto.LecturerExamDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class ExamTabsDto {

    private final List<LecturerExamDto> live;
    private final List<LecturerExamDto> upcoming;
    private final List<LecturerExamDto> completed;
}
