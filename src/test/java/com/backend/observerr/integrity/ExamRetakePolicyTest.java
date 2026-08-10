package com.backend.observerr.integrity;

import com.backend.observerr.auth.model.User;
import com.backend.observerr.exam.model.Exam;
import com.backend.observerr.exam.model.ExamStatus;
import com.backend.observerr.exam.repository.ExamEnrollmentRepository;
import com.backend.observerr.exam.repository.ExamRepository;
import com.backend.observerr.exam.repository.ExamResultRepository;
import com.backend.observerr.exam.service.ExamStudentBlockService;
import com.backend.observerr.integrity.model.ExamSessionStatus;
import com.backend.observerr.integrity.repository.ExamSessionRepository;
import com.backend.observerr.integrity.repository.IntegrityEventRepository;
import com.backend.observerr.notification.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExamRetakePolicyTest {

    @Mock private ExamRepository examRepository;
    @Mock private ExamEnrollmentRepository examEnrollmentRepository;
    @Mock private ExamResultRepository examResultRepository;
    @Mock private ExamSessionRepository examSessionRepository;
    @Mock private IntegrityEventRepository integrityEventRepository;
    @Mock private IntegrityScoringPolicy scoringPolicy;
    @Mock private ExamStudentBlockService blockService;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private IntegritySessionService integritySessionService;

    private User student;
    private Exam exam;

    @BeforeEach
    void setUp() {
        student = User.builder().id(5L).institutionalId("STU-1").build();
        Instant now = Instant.now();
        exam = Exam.builder()
                .id(10L)
                .title("Midterm")
                .lecturerId(3L)
                .published(true)
                .status(ExamStatus.LIVE)
                .startTime(now.minusSeconds(60))
                .endTime(now.plusSeconds(3600))
                .durationMinutes(60)
                .webcamMonitoring(true)
                .allowRetake(false)
                .build();
    }

    @Test
    void startSession_blocksSecondAttemptWhenRetakesDisabled() {
        when(examRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(exam));
        when(examEnrollmentRepository.existsByExamIdAndStudentId(10L, 5L)).thenReturn(true);
        when(examSessionRepository.findByExamIdAndStudentIdAndStatus(
                10L, 5L, ExamSessionStatus.IN_PROGRESS)).thenReturn(Optional.empty());
        when(examSessionRepository.existsByExamIdAndStudentIdAndStatus(
                10L, 5L, ExamSessionStatus.COMPLETED)).thenReturn(true);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> integritySessionService.startSession(student, 10L, null));

        assertEquals(409, ex.getStatusCode().value());
        verify(examSessionRepository, never()).save(any());
    }

    @Test
    void startSession_allowsRetakeWhenLecturerEnabledIt() {
        exam.setAllowRetake(true);
        when(examRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(exam));
        when(examEnrollmentRepository.existsByExamIdAndStudentId(10L, 5L)).thenReturn(true);
        when(examSessionRepository.findByExamIdAndStudentIdAndStatus(
                10L, 5L, ExamSessionStatus.IN_PROGRESS)).thenReturn(Optional.empty());
        when(examSessionRepository.existsByExamIdAndStudentIdAndStatus(
                eq(10L), eq(5L), eq(ExamSessionStatus.COMPLETED))).thenReturn(true);
        when(examSessionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = integritySessionService.startSession(student, 10L, null);

        assertEquals(ExamSessionStatus.IN_PROGRESS.name(), response.getStatus());
        verify(examSessionRepository).save(any());
    }
}
