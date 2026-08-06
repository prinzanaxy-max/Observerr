package com.backend.observerr.exam;

import com.backend.observerr.exam.model.Exam;
import com.backend.observerr.exam.model.ExamDisplayStatus;
import com.backend.observerr.exam.model.ExamStatus;

import java.time.Duration;
import java.time.Instant;

/**
 * Resolves API display status from persisted {@link ExamStatus} first, then the
 * scheduled time window for exams that are still {@link ExamStatus#SCHEDULED}.
 */
public final class ExamDisplayStatusResolver {

    private ExamDisplayStatusResolver() {
    }

    public static ExamDisplayStatus resolve(Exam exam) {
        if (exam.getStatus() == ExamStatus.ENDED || exam.getStatus() == ExamStatus.CANCELLED) {
            return ExamDisplayStatus.COMPLETED;
        }
        if (exam.getStatus() == ExamStatus.LIVE) {
            return ExamDisplayStatus.LIVE;
        }

        Instant now = Instant.now();
        Instant start = exam.getStartTime();
        if (start == null) {
            return ExamDisplayStatus.UPCOMING;
        }
        Instant end = start.plusSeconds(resolveDurationMinutes(exam) * 60L);

        if (!now.isBefore(start) && now.isBefore(end)) {
            return ExamDisplayStatus.LIVE;
        }
        if (now.isBefore(start)) {
            return ExamDisplayStatus.UPCOMING;
        }
        return ExamDisplayStatus.COMPLETED;
    }

    public static int resolveDurationMinutes(Exam exam) {
        if (exam.getDurationMinutes() != null && exam.getDurationMinutes() > 0) {
            return exam.getDurationMinutes();
        }
        if (exam.getEndTime() != null && exam.getStartTime() != null) {
            long minutes = Duration.between(exam.getStartTime(), exam.getEndTime()).toMinutes();
            if (minutes > 0) {
                return (int) minutes;
            }
        }
        return 120;
    }
}
