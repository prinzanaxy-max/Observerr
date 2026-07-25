package com.backend.observerr.notification;

import com.backend.observerr.exam.model.Exam;
import com.backend.observerr.exam.repository.ExamEnrollmentRepository;
import com.backend.observerr.exam.repository.ExamRepository;
import com.backend.observerr.notification.dto.NotificationPayload;
import com.backend.observerr.notification.model.DeviceToken;
import com.backend.observerr.notification.repository.DeviceTokenRepository;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.WebpushConfig;
import com.google.firebase.messaging.WebpushFcmOptions;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    static final int MULTICAST_BATCH_SIZE = 500;

    private final FcmClient fcmClient;
    private final ExamRepository examRepository;
    private final ExamEnrollmentRepository enrollmentRepository;
    private final DeviceTokenRepository deviceTokenRepository;

    @Value("${app.frontend-base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    public void notifyStudentsExamStarted(Long examId) {
        Exam exam = loadExam(examId);
        List<Long> studentIds = enrollmentRepository.findStudentIdsByExamId(examId);

        if (studentIds.isEmpty()) {
            log.info("No enrolled students for examId={} — skipping student notifications", examId);
            return;
        }

        List<DeviceToken> tokens = deviceTokenRepository.findByUserIdIn(studentIds);
        if (tokens.isEmpty()) {
            log.info("No device tokens for enrolled students on examId={}", examId);
            return;
        }

        NotificationPayload payload = buildStudentExamStartMessage(exam);
        sendMulticastInBatches(tokens, payload, "EXAM_STARTED");
        log.info("Dispatched student exam-start notifications examId={} tokenCount={}", examId, tokens.size());
    }

    public void notifyLecturerExamStarted(Long examId) {
        Exam exam = loadExam(examId);
        long studentCount = enrollmentRepository.countByExamId(examId);
        List<DeviceToken> tokens = deviceTokenRepository.findByUserId(exam.getLecturerId());

        if (tokens.isEmpty()) {
            log.info("No device tokens for lecturerId={} examId={}", exam.getLecturerId(), examId);
            return;
        }

        NotificationPayload payload = buildLecturerExamStartMessage(exam, studentCount);
        for (DeviceToken deviceToken : tokens) {
            sendMessage(deviceToken, payload);
        }
        log.info("Dispatched lecturer exam-start notifications examId={} lecturerId={} tokenCount={}",
                examId, exam.getLecturerId(), tokens.size());
    }

    public void sendHighRiskAlert(Long userId, Long examId, String alertMessage) {
        List<DeviceToken> tokens = deviceTokenRepository.findByUserId(userId);
        if (tokens.isEmpty()) {
            log.info("No device tokens for high-risk alert userId={} examId={}", userId, examId);
            return;
        }

        NotificationPayload payload = NotificationPayload.builder()
                .title("High Risk Alert")
                .body(alertMessage)
                .data(Map.of(
                        "type", "HIGH_RISK",
                        "examId", String.valueOf(examId)
                ))
                .webPushLink(frontendBaseUrl + "/lecturer/exams/" + examId + "/live")
                .build();

        for (DeviceToken deviceToken : tokens) {
            sendMessage(deviceToken, payload);
        }
        log.warn("High-risk alert sent userId={} examId={} tokenCount={}", userId, examId, tokens.size());
    }

    private Exam loadExam(Long examId) {
        return examRepository.findById(examId)
                .orElseThrow(() -> new EntityNotFoundException("Exam not found: " + examId));
    }

    private NotificationPayload buildStudentExamStartMessage(Exam exam) {
        return NotificationPayload.builder()
                .title("Exam Started")
                .body(exam.getTitle() + " has started. Good luck!")
                .data(Map.of(
                        "type", "EXAM_STARTED",
                        "examId", String.valueOf(exam.getId())
                ))
                .webPushLink(frontendBaseUrl + "/student/exams/" + exam.getId())
                .build();
    }

    private NotificationPayload buildLecturerExamStartMessage(Exam exam, long studentCount) {
        return NotificationPayload.builder()
                .title("Exam Live")
                .body(exam.getTitle() + " is now live — " + studentCount + " students joining")
                .data(Map.of(
                        "type", "EXAM_LIVE",
                        "examId", String.valueOf(exam.getId())
                ))
                .webPushLink(frontendBaseUrl + "/lecturer/exams/" + exam.getId() + "/live")
                .build();
    }

    private void sendMulticastInBatches(List<DeviceToken> deviceTokens, NotificationPayload payload, String type) {
        List<String> tokenValues = deviceTokens.stream().map(DeviceToken::getToken).toList();

        for (int index = 0; index < tokenValues.size(); index += MULTICAST_BATCH_SIZE) {
            int end = Math.min(index + MULTICAST_BATCH_SIZE, tokenValues.size());
            List<String> batch = tokenValues.subList(index, end);
            sendMulticastBatch(deviceTokens, batch, payload, type);
        }
    }

    private void sendMulticastBatch(
            List<DeviceToken> allTokens,
            List<String> batchTokenValues,
            NotificationPayload payload,
            String type) {
        if (!fcmClient.isEnabled()) {
            log.warn("FCM disabled — skipping {} multicast batch size={}", type, batchTokenValues.size());
            return;
        }

        MulticastMessage message = buildMulticastMessage(batchTokenValues, payload);
        try {
            BatchResponse response = fcmClient.sendEachForMulticast(message);
            if (response == null) {
                return;
            }
            handleBatchResponse(allTokens, batchTokenValues, response, type);
        } catch (FirebaseMessagingException ex) {
            log.error("FCM multicast failed type={} batchSize={} error={}", type, batchTokenValues.size(), ex.getMessage());
        }
    }

    private void sendMessage(DeviceToken deviceToken, NotificationPayload payload) {
        if (!fcmClient.isEnabled()) {
            log.warn("FCM disabled — skipping notification to userId={}", deviceToken.getUserId());
            return;
        }

        Message message = buildSingleMessage(deviceToken.getToken(), payload);
        try {
            fcmClient.send(message);
            log.debug("FCM notification sent userId={} tokenId={}", deviceToken.getUserId(), deviceToken.getId());
        } catch (FirebaseMessagingException ex) {
            log.warn("FCM send failed userId={} tokenId={} error={}",
                    deviceToken.getUserId(), deviceToken.getId(), ex.getMessage());
            cleanupDeadToken(deviceToken.getToken(), ex);
        }
    }

    private void handleBatchResponse(
            List<DeviceToken> allTokens,
            List<String> batchTokenValues,
            BatchResponse response,
            String type) {
        log.info("FCM multicast batch type={} success={} failure={}",
                type, response.getSuccessCount(), response.getFailureCount());

        if (response.getFailureCount() == 0) {
            return;
        }

        var responses = response.getResponses();
        for (int i = 0; i < responses.size(); i++) {
            if (responses.get(i).isSuccessful()) {
                continue;
            }
            FirebaseMessagingException exception = responses.get(i).getException();
            String failedToken = batchTokenValues.get(i);
            log.warn("FCM multicast token failed type={} error={}", type,
                    exception != null ? exception.getMessage() : "unknown");
            cleanupDeadToken(failedToken, exception);
        }
    }

    private void cleanupDeadToken(String token, FirebaseMessagingException exception) {
        if (!isDeadTokenError(exception)) {
            return;
        }
        deviceTokenRepository.findByToken(token).ifPresent(existing -> {
            deviceTokenRepository.delete(existing);
            log.info("Removed dead FCM token userId={} tokenId={}", existing.getUserId(), existing.getId());
        });
    }

    private boolean isDeadTokenError(FirebaseMessagingException exception) {
        if (exception == null || exception.getMessagingErrorCode() == null) {
            return false;
        }
        return switch (exception.getMessagingErrorCode()) {
            case INVALID_ARGUMENT, UNREGISTERED -> true;
            default -> false;
        };
    }

    private Message buildSingleMessage(String token, NotificationPayload payload) {
        return Message.builder()
                .setToken(token)
                .setNotification(buildNotification(payload))
                .putAllData(safeData(payload.getData()))
                .setWebpushConfig(buildWebPushConfig(payload.getWebPushLink()))
                .build();
    }

    private MulticastMessage buildMulticastMessage(List<String> tokens, NotificationPayload payload) {
        return MulticastMessage.builder()
                .addAllTokens(tokens)
                .setNotification(buildNotification(payload))
                .putAllData(safeData(payload.getData()))
                .setWebpushConfig(buildWebPushConfig(payload.getWebPushLink()))
                .build();
    }

    private Notification buildNotification(NotificationPayload payload) {
        return Notification.builder()
                .setTitle(payload.getTitle())
                .setBody(payload.getBody())
                .build();
    }

    private WebpushConfig buildWebPushConfig(String link) {
        return WebpushConfig.builder()
                .setFcmOptions(WebpushFcmOptions.builder().setLink(link).build())
                .build();
    }

    private Map<String, String> safeData(Map<String, String> data) {
        return data == null ? new HashMap<>() : new HashMap<>(data);
    }
}
