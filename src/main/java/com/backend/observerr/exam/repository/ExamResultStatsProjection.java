package com.backend.observerr.exam.repository;

public interface ExamResultStatsProjection {
    long getExamsCompleted();

    Double getAverageIntegrity();

    long getVerifiedSessions();

    long getUnderReview();
}
