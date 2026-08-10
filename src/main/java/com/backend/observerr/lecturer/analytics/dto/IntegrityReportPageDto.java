package com.backend.observerr.lecturer.analytics.dto;

import lombok.*;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntegrityReportPageDto {
    private List<IntegrityReportEventDto> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private List<String> eventTypes;
}
