package com.backend.observerr.lecturer.analytics.repository;

import java.time.Instant;
import java.util.UUID;

public interface IntegrityReportRow {
    Long getId();
    UUID getSessionId();
    Long getStudentId();
    String getStudentName();
    Long getExamId();
    String getExamTitle();
    String getEventType();
    String getSeverity();
    Instant getOccurredAt();
    Integer getPointsDeducted();
}
