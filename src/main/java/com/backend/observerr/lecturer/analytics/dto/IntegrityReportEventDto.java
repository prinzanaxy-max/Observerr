package com.backend.observerr.lecturer.analytics.dto;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntegrityReportEventDto {
    private Long id;
    private String sessionId;
    private Long studentId;
    private String studentName;
    private Long examId;
    private String examTitle;
    private String eventType;
    private String severity;
    private String occurredAt;
    private Integer pointsDeducted;
}
