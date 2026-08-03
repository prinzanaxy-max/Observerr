package com.backend.observerr.notification;

import com.backend.observerr.exam.model.Exam;
import com.backend.observerr.exam.model.ExamStatus;
import com.backend.observerr.exam.repository.ExamEnrollmentRepository;
import com.backend.observerr.exam.repository.ExamRepository;
import com.backend.observerr.notification.model.DeviceToken;
import com.backend.observerr.notification.model.UserNotification;
import com.backend.observerr.notification.repository.DeviceTokenRepository;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private WebPushClient webPushClient;

    @Mock
    private ExamRepository examRepository;

    @Mock
    private ExamEnrollmentRepository enrollmentRepository;

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    @Mock
    private NotificationInboxService inboxService;

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
    void notifyStudentsExamStarted_skipsWhenNoEnrolledStudents() throws Exception {
        when(examRepository.findById(42L)).thenReturn(Optional.of(exam));
        when(enrollmentRepository.findStudentIdsByExamId(42L)).thenReturn(List.of());

        notificationService.notifyStudentsExamStarted(42L);

        verify(deviceTokenRepository, never()).findByUserIdIn(any());
        verify(webPushClient, never()).send(any(), anyString());
    }

    @Test
    void notifyStudentsExamStarted_skipsWhenNoDeviceTokens() throws Exception {
        when(examRepository.findById(42L)).thenReturn(Optional.of(exam));
        when(enrollmentRepository.findStudentIdsByExamId(42L)).thenReturn(List.of(1L, 2L));
        when(deviceTokenRepository.findByUserIdIn(List.of(1L, 2L))).thenReturn(List.of());

        notificationService.notifyStudentsExamStarted(42L);

        verify(webPushClient, never()).send(any(), anyString());
    }

    @Test
    void notifyStudentsExamStarted_sendsToEachSubscription() throws Exception {
        when(examRepository.findById(42L)).thenReturn(Optional.of(exam));
        when(enrollmentRepository.findStudentIdsByExamId(42L)).thenReturn(List.of(1L));

        List<DeviceToken> tokens = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            tokens.add(DeviceToken.builder()
                    .id((long) i)
                    .userId(1L)
                    .endpoint("https://push.example/" + i)
                    .p256dh("p256dh-" + i)
                    .auth("auth-" + i)
                    .build());
        }
        when(deviceTokenRepository.findByUserIdIn(List.of(1L))).thenReturn(tokens);
        when(webPushClient.isEnabled()).thenReturn(true);
        when(webPushClient.send(any(), anyString())).thenReturn(201);

        notificationService.notifyStudentsExamStarted(42L);

        verify(webPushClient, times(3)).send(any(), anyString());
    }

    @Test
    void notifyLecturerExamStarted_sendsToLecturerTokens() throws Exception {
        when(examRepository.findById(42L)).thenReturn(Optional.of(exam));
        when(enrollmentRepository.countByExamId(42L)).thenReturn(12L);
        when(deviceTokenRepository.findByUserId(7L)).thenReturn(List.of(
                DeviceToken.builder()
                        .id(1L)
                        .userId(7L)
                        .endpoint("https://push.example/lecturer")
                        .p256dh("p256dh")
                        .auth("auth")
                        .build()
        ));
        when(webPushClient.isEnabled()).thenReturn(true);
        when(webPushClient.send(any(), anyString())).thenReturn(201);

        notificationService.notifyLecturerExamStarted(42L);

        verify(webPushClient).send(any(), anyString());
    }

    @Test
    void notifyLecturerExamStarted_skipsWhenNoTokens() throws Exception {
        when(examRepository.findById(42L)).thenReturn(Optional.of(exam));
        when(enrollmentRepository.countByExamId(42L)).thenReturn(0L);
        when(deviceTokenRepository.findByUserId(7L)).thenReturn(List.of());

        notificationService.notifyLecturerExamStarted(42L);

        verify(webPushClient, never()).send(any(), anyString());
    }
}
