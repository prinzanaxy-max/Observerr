package com.backend.observerr.exam.service;

import com.backend.observerr.exam.model.Exam;
import com.backend.observerr.exam.model.ExamStatus;
import com.backend.observerr.exam.repository.ExamRepository;
import com.backend.observerr.notification.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

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
                .startTime(Instant.now().minusSeconds(60))
                .startNotificationsSent(false)
                .build();
    }

    @Test
    void transitionExamToLive_sendsBothNotificationsOnce() {
        when(examRepository.findById(10L)).thenReturn(Optional.of(exam));
        when(examRepository.save(any(Exam.class))).thenAnswer(invocation -> invocation.getArgument(0));

        examLifecycleService.transitionExamToLive(10L);

        verify(notificationService).notifyStudentsExamStarted(10L);
        verify(notificationService).notifyLecturerExamStarted(10L);
        verify(examRepository, org.mockito.Mockito.times(2)).save(exam);
    }

    @Test
    void transitionExamToLive_skipsWhenAlreadySent() {
        exam.setStartNotificationsSent(true);
        when(examRepository.findById(10L)).thenReturn(Optional.of(exam));

        examLifecycleService.transitionExamToLive(10L);

        verify(notificationService, never()).notifyStudentsExamStarted(any());
        verify(notificationService, never()).notifyLecturerExamStarted(any());
        verify(examRepository, never()).save(any());
    }

    @Test
    void transitionExamToLive_skipsWhenNotScheduled() {
        exam.setStatus(ExamStatus.LIVE);
        when(examRepository.findById(10L)).thenReturn(Optional.of(exam));

        examLifecycleService.transitionExamToLive(10L);

        verify(notificationService, never()).notifyStudentsExamStarted(any());
        verify(notificationService, never()).notifyLecturerExamStarted(any());
        verify(examRepository, never()).save(any());
    }
}
