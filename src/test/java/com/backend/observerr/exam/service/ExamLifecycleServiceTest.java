package com.backend.observerr.exam.service;

import com.backend.observerr.exam.model.Exam;
import com.backend.observerr.exam.model.ExamStatus;
import com.backend.observerr.exam.repository.ExamRepository;
import com.backend.observerr.integrity.model.ExamSession;
import com.backend.observerr.integrity.model.ExamSessionStatus;
import com.backend.observerr.integrity.repository.ExamSessionRepository;
import com.backend.observerr.notification.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExamLifecycleServiceTest {

    @Mock
    private ExamRepository examRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ExamSessionRepository sessionRepository;

    @InjectMocks
    private ExamLifecycleService examLifecycleService;

    private Exam exam;

    @BeforeEach
    void setUp() {
        exam = Exam.builder()
                .id(10L)
                .title("Final Exam")
                .lecturerId(3L)
                .status(ExamStatus.SCHEDULED)
                .published(true)
                .startTime(Instant.now().minusSeconds(60))
                .durationMinutes(90)
                .startNotificationsSent(false)
                .build();
    }

    @Test
    void transitionExamToLive_sendsBothNotificationsOnce() {
        when(examRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(exam));
        when(examRepository.save(any(Exam.class))).thenAnswer(invocation -> invocation.getArgument(0));

        examLifecycleService.transitionExamToLive(10L);

        verify(notificationService).notifyStudentsExamStarted(10L);
        verify(notificationService).notifyLecturerExamStarted(10L);
        verify(examRepository, org.mockito.Mockito.times(2)).save(exam);
    }

    @Test
    void transitionExamToLive_skipsWhenAlreadySent() {
        exam.setStartNotificationsSent(true);
        when(examRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(exam));

        examLifecycleService.transitionExamToLive(10L);

        verify(notificationService, never()).notifyStudentsExamStarted(any());
        verify(notificationService, never()).notifyLecturerExamStarted(any());
        verify(examRepository, never()).save(any());
    }

    @Test
    void transitionExamToLive_skipsWhenNotScheduled() {
        exam.setStatus(ExamStatus.LIVE);
        when(examRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(exam));

        examLifecycleService.transitionExamToLive(10L);

        verify(notificationService, never()).notifyStudentsExamStarted(any());
        verify(notificationService, never()).notifyLecturerExamStarted(any());
        verify(examRepository, never()).save(any());
    }

    @Test
    void transitionExamToLive_pullsStartTimeForwardWhenStartingEarly() {
        Instant futureStart = Instant.now().plusSeconds(3600);
        exam.setStartTime(futureStart);
        exam.setEndTime(futureStart.plusSeconds(90 * 60L));
        when(examRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(exam));
        when(examRepository.save(any(Exam.class))).thenAnswer(invocation -> invocation.getArgument(0));

        examLifecycleService.transitionExamToLive(10L);

        assertEquals(ExamStatus.LIVE, exam.getStatus());
        assertTrue(exam.getStartTime().isBefore(futureStart));
        assertTrue(exam.getEndTime().isAfter(exam.getStartTime()));
        verify(notificationService).notifyStudentsExamStarted(10L);
    }

    @Test
    void endExam_sealsInProgressSessionsAndMarksEnded() {
        exam.setStatus(ExamStatus.LIVE);
        ExamSession session = ExamSession.builder()
                .id(java.util.UUID.randomUUID())
                .examId(10L)
                .studentId(5L)
                .status(ExamSessionStatus.IN_PROGRESS)
                .startingScore((short) 100)
                .totalDeductions(15)
                .build();
        when(examRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(exam));
        when(sessionRepository.findByExamId(10L)).thenReturn(List.of(session));
        when(examRepository.save(any(Exam.class))).thenAnswer(invocation -> invocation.getArgument(0));

        examLifecycleService.endExam(10L);

        assertEquals(ExamStatus.ENDED, exam.getStatus());
        assertEquals(ExamSessionStatus.COMPLETED, session.getStatus());
        assertEquals((short) 85, session.getFinalScore());
        assertTrue(session.isRequiresReview());
        verify(notificationService).notifyExamEnded(10L);
    }
}
