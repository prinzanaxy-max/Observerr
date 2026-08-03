package com.backend.observerr.notification;

import com.backend.observerr.exam.model.Exam;
import com.backend.observerr.exam.repository.ExamEnrollmentRepository;
import com.backend.observerr.exam.repository.ExamRepository;
import com.backend.observerr.notification.dto.NotificationPayload;
import com.backend.observerr.notification.model.DeviceToken;
import com.backend.observerr.notification.repository.DeviceTokenRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final WebPushClient webPushClient;
    private final ExamRepository examRepository;
    private final ExamEnrollmentRepository enrollmentRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final NotificationInboxService inboxService;

    @Value("${app.frontend-base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    public void notifyStudentsExamStarted(Long examId) {
        Exam exam = loadExam(examId);
        List<Long> studentIds = enrollmentRepository.findStudentIdsByExamId(examId);

        NotificationPayload payload = buildStudentExamStartMessage(exam);
        List<Long> permittedStudentIds = new ArrayList<>();
        for (Long studentId : studentIds) {
            if (inboxService.create(studentId, "EXAM", payload.getTitle(), payload.getBody(),
                    payload.getWebPushLink(), "exam-start:" + examId + ":" + studentId) != null) {
                permittedStudentIds.add(studentId);
            }
        }
        if (!permittedStudentIds.isEmpty()) {
            for (DeviceToken subscription : deviceTokenRepository.findByUserIdIn(permittedStudentIds)) {
                sendPush(subscription, payload);
            }
        }
    }

    public void notifyLecturerExamStarted(Long examId) {
        Exam exam = loadExam(examId);
        long studentCount = enrollmentRepository.countByExamId(examId);
        NotificationPayload payload = buildLecturerExamStartMessage(exam, studentCount);
        emit(exam.getLecturerId(), "EXAM", payload, "exam-live:" + examId);
    }

    public void sendHighRiskAlert(Long userId, Long examId, String alertMessage) {
        NotificationPayload payload = NotificationPayload.builder()
                .title("High Risk Alert")
                .body(alertMessage)
                .data(Map.of(
                        "type", "HIGH_RISK",
                        "examId", String.valueOf(examId)
                ))
                .webPushLink(frontendBaseUrl + "/lecturer/exams/" + examId + "/live")
                .build();
        emit(userId, "INTEGRITY", payload, null);
    }

    public void notifyExamEnded(Long examId) {
        Exam exam = loadExam(examId);
        NotificationPayload payload = NotificationPayload.builder()
                .title("Exam Ended").body(exam.getTitle() + " has ended.")
                .data(Map.of("type", "EXAM_ENDED", "examId", examId.toString()))
                .webPushLink(frontendBaseUrl + "/student/exams").build();
        for (Long studentId : enrollmentRepository.findStudentIdsByExamId(examId)) {
            emit(studentId, "EXAM", payload, "exam-end:" + examId + ":" + studentId);
        }
        emit(exam.getLecturerId(), "EXAM", payload, "exam-end:" + examId + ":lecturer");
    }

    public void notifyStudentCompleted(Long lecturerId, Long studentId, Long examId) {
        NotificationPayload payload = NotificationPayload.builder()
                .title("Student Completed Exam")
                .body("A student submitted exam #" + examId + ".")
                .data(Map.of("type", "STUDENT_COMPLETED", "examId", examId.toString(),
                        "studentId", studentId.toString()))
                .webPushLink(frontendBaseUrl + "/lecturer/exams/" + examId + "/results").build();
        emit(lecturerId, "EXAM", payload, "completion:" + examId + ":" + studentId);
    }

    public void notifyResultReleased(Long studentId, Long examId, Long resultId) {
        NotificationPayload payload = NotificationPayload.builder()
                .title("Result Released").body("Your exam result is now available.")
                .data(Map.of("type", "RESULT_RELEASED", "examId", examId.toString(),
                        "resultId", resultId.toString()))
                .webPushLink(frontendBaseUrl + "/student/results/" + resultId).build();
        emit(studentId, "RESULT", payload, "result-release:" + resultId);
    }

    private void emit(Long userId, String category, NotificationPayload payload, String dedupeKey) {
        if (inboxService.create(userId, category, payload.getTitle(), payload.getBody(),
                payload.getWebPushLink(), dedupeKey) == null) {
            return;
        }
        for (DeviceToken subscription : deviceTokenRepository.findByUserId(userId)) {
            sendPush(subscription, payload);
        }
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

    private void sendPush(DeviceToken subscription, NotificationPayload payload) {
        if (!webPushClient.isEnabled()) {
            log.warn("Web Push disabled — skipping notification to userId={}", subscription.getUserId());
            return;
        }

        try {
            int status = webPushClient.send(subscription, toJsonPayload(payload));
            if (status == 404 || status == 410) {
                cleanupDeadSubscription(subscription);
                return;
            }
            if (status >= 200 && status < 300) {
                log.debug("Web Push sent userId={} tokenId={} status={}",
                        subscription.getUserId(), subscription.getId(), status);
            } else {
                log.warn("Web Push unexpected status userId={} tokenId={} status={}",
                        subscription.getUserId(), subscription.getId(), status);
            }
        } catch (Exception ex) {
            log.warn("Web Push send failed userId={} tokenId={} error={}",
                    subscription.getUserId(), subscription.getId(), ex.getMessage());
            if (isGoneError(ex)) {
                cleanupDeadSubscription(subscription);
            }
        }
    }

    private String toJsonPayload(NotificationPayload payload) {
        StringBuilder dataJson = new StringBuilder("{");
        Map<String, String> data = payload.getData();
        if (data != null && !data.isEmpty()) {
            boolean first = true;
            for (Map.Entry<String, String> entry : data.entrySet()) {
                if (!first) {
                    dataJson.append(',');
                }
                first = false;
                dataJson.append(jsonString(entry.getKey())).append(':').append(jsonString(entry.getValue()));
            }
        }
        dataJson.append('}');

        return "{"
                + "\"title\":" + jsonString(payload.getTitle()) + ","
                + "\"body\":" + jsonString(payload.getBody()) + ","
                + "\"deepLink\":" + jsonString(payload.getWebPushLink()) + ","
                + "\"data\":" + dataJson
                + "}";
    }

    private static String jsonString(String value) {
        if (value == null) {
            return "null";
        }
        String escaped = value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
        return "\"" + escaped + "\"";
    }

    private void cleanupDeadSubscription(DeviceToken subscription) {
        deviceTokenRepository.delete(subscription);
        log.info("Removed expired Web Push subscription userId={} tokenId={}",
                subscription.getUserId(), subscription.getId());
    }

    private boolean isGoneError(Exception ex) {
        String message = ex.getMessage() == null ? "" : ex.getMessage();
        return message.contains("410") || message.contains("404") || message.toLowerCase().contains("gone");
    }
}
