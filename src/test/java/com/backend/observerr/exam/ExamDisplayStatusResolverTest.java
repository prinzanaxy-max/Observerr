package com.backend.observerr.exam;

import com.backend.observerr.exam.model.Exam;
import com.backend.observerr.exam.model.ExamDisplayStatus;
import com.backend.observerr.exam.model.ExamStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExamDisplayStatusResolverTest {

    @Test
    void endedExamIsCompletedEvenInsideScheduledWindow() {
        Instant now = Instant.now();
        Exam exam = Exam.builder()
                .status(ExamStatus.ENDED)
                .startTime(now.minusSeconds(60))
                .durationMinutes(120)
                .endTime(now)
                .build();

        assertEquals(ExamDisplayStatus.COMPLETED, ExamDisplayStatusResolver.resolve(exam));
    }

    @Test
    void liveExamIsLiveEvenBeforeOriginalStartTime() {
        Instant now = Instant.now();
        Exam exam = Exam.builder()
                .status(ExamStatus.LIVE)
                .startTime(now.plusSeconds(3600))
                .durationMinutes(90)
                .build();

        assertEquals(ExamDisplayStatus.LIVE, ExamDisplayStatusResolver.resolve(exam));
    }

    @Test
    void scheduledFutureExamIsUpcoming() {
        Instant now = Instant.now();
        Exam exam = Exam.builder()
                .status(ExamStatus.SCHEDULED)
                .startTime(now.plusSeconds(3600))
                .durationMinutes(90)
                .build();

        assertEquals(ExamDisplayStatus.UPCOMING, ExamDisplayStatusResolver.resolve(exam));
    }
}
