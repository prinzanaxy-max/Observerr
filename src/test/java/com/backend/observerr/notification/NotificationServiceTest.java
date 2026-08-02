package com.backend.observerr.notification;

import com.backend.observerr.exam.model.Exam;
import com.backend.observerr.exam.model.ExamStatus;
import com.backend.observerr.exam.repository.ExamEnrollmentRepository;
import com.backend.observerr.exam.repository.ExamRepository;
import com.backend.observerr.notification.model.DeviceToken;
import com.backend.observerr.notification.repository.DeviceTokenRepository;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;
import com.backend.observerr.notification.model.UserNotification;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private FcmClient fcmClient;

    @Mock
    private ExamRepository examRepository;

    @Mock
    private ExamEnrollmentRepository enrollmentRepository;

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    @Mock
    private NotificationInboxService inboxService;

    @Mock
    private BatchResponse batchResponse;

    @InjectMocks
    private NotificationService notificationService;

    private Exam exam;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(notificationService, "frontendBaseUrl", "https://app.example.com");
        lenient().when(inboxService.create(any(), any(), any(), any(), any(), any()))
                .thenReturn(UserNotification.builder().id(1L).build());

        exam = Exam.builder()
                .id(42L)
                .title("Midterm CS101")
                .lecturerId(7L)
                .status(ExamStatus.LIVE)
                .startTime(Instant.now())
                .build();
    }

    @Test
    void notifyStudentsExamStarted_skipsWhenNoEnrolledStudents() throws FirebaseMessagingException {
        when(examRepository.findById(42L)).thenReturn(Optional.of(exam));
        when(enrollmentRepository.findStudentIdsByExamId(42L)).thenReturn(List.of());

        notificationService.notifyStudentsExamStarted(42L);

        verify(deviceTokenRepository, never()).findByUserIdIn(any());
        verify(fcmClient, never()).sendEachForMulticast(any());
    }

    @Test
    void notifyStudentsExamStarted_skipsWhenNoDeviceTokens() throws FirebaseMessagingException {
        when(examRepository.findById(42L)).thenReturn(Optional.of(exam));
        when(enrollmentRepository.findStudentIdsByExamId(42L)).thenReturn(List.of(1L, 2L));
        when(deviceTokenRepository.findByUserIdIn(List.of(1L, 2L))).thenReturn(List.of());

        notificationService.notifyStudentsExamStarted(42L);

        verify(fcmClient, never()).sendEachForMulticast(any());
    }

    @Test
    void notifyStudentsExamStarted_batchesTokensInGroupsOfFiveHundred() throws FirebaseMessagingException {
        when(examRepository.findById(42L)).thenReturn(Optional.of(exam));
        when(enrollmentRepository.findStudentIdsByExamId(42L)).thenReturn(List.of(1L));

        List<DeviceToken> tokens = new ArrayList<>();
        for (int i = 0; i < 501; i++) {
            tokens.add(DeviceToken.builder().id((long) i).userId(1L).token("token-" + i).build());
        }
        when(deviceTokenRepository.findByUserIdIn(List.of(1L))).thenReturn(tokens);
        when(fcmClient.isEnabled()).thenReturn(true);
        when(fcmClient.sendEachForMulticast(any(MulticastMessage.class))).thenReturn(batchResponse);
        when(batchResponse.getFailureCount()).thenReturn(0);

        notificationService.notifyStudentsExamStarted(42L);

        verify(fcmClient, times(2)).sendEachForMulticast(any(MulticastMessage.class));
    }

    @Test
    void notifyLecturerExamStarted_sendsToLecturerTokens() throws FirebaseMessagingException {
        when(examRepository.findById(42L)).thenReturn(Optional.of(exam));
        when(enrollmentRepository.countByExamId(42L)).thenReturn(12L);
        when(deviceTokenRepository.findByUserId(7L)).thenReturn(List.of(
                DeviceToken.builder().id(1L).userId(7L).token("lecturer-token").build()
        ));
        when(fcmClient.isEnabled()).thenReturn(true);

        notificationService.notifyLecturerExamStarted(42L);

        verify(fcmClient).send(any(Message.class));
    }

    @Test
    void notifyLecturerExamStarted_skipsWhenNoTokens() throws FirebaseMessagingException {
        when(examRepository.findById(42L)).thenReturn(Optional.of(exam));
        when(enrollmentRepository.countByExamId(42L)).thenReturn(0L);
        when(deviceTokenRepository.findByUserId(7L)).thenReturn(List.of());

        notificationService.notifyLecturerExamStarted(42L);

        verify(fcmClient, never()).send(any(Message.class));
    }
}
