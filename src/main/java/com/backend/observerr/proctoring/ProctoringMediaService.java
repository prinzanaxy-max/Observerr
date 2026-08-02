package com.backend.observerr.proctoring;

import com.backend.observerr.auth.model.User;
import com.backend.observerr.exam.model.Exam;
import com.backend.observerr.exam.model.ExamStatus;
import com.backend.observerr.exam.repository.ExamEnrollmentRepository;
import com.backend.observerr.exam.repository.ExamRepository;
import com.backend.observerr.integrity.model.ExamSession;
import com.backend.observerr.integrity.model.ExamSessionStatus;
import com.backend.observerr.integrity.repository.ExamSessionRepository;
import com.backend.observerr.proctoring.dto.LiveKitTokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProctoringMediaService {

    private final ExamRepository examRepository;
    private final ExamEnrollmentRepository examEnrollmentRepository;
    private final ExamSessionRepository examSessionRepository;
    private final LiveKitTokenService liveKitTokenService;

    @Transactional(readOnly = true)
    public LiveKitTokenResponse issueStudentToken(User student, UUID sessionId) {
        ExamSession session = examSessionRepository.findByIdAndStudentId(sessionId, student.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
        if (session.getStatus() != ExamSessionStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Session is not in progress");
        }

        Exam exam = examRepository.findById(session.getExamId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam not found"));
        requireLiveProctoredExam(exam);
        if (!examEnrollmentRepository.existsByExamIdAndStudentId(exam.getId(), student.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Student is not enrolled in this exam");
        }

        return liveKitTokenService.createPublisherToken(exam.getId(), sessionId.toString());
    }

    @Transactional(readOnly = true)
    public LiveKitTokenResponse issueLecturerToken(User lecturer, Long examId) {
        Exam exam = examRepository.findByIdAndLecturerId(examId, lecturer.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam not found"));
        requireLiveProctoredExam(exam);
        return liveKitTokenService.createSubscriberToken(examId, lecturer.getId());
    }

    private void requireLiveProctoredExam(Exam exam) {
        Instant now = Instant.now();
        Instant effectiveEnd = exam.getEndTime();
        if (effectiveEnd == null && exam.getDurationMinutes() != null && exam.getStartTime() != null) {
            effectiveEnd = exam.getStartTime().plusSeconds(exam.getDurationMinutes() * 60L);
        }

        if (!exam.isPublished()
                || exam.getStatus() != ExamStatus.LIVE
                || exam.getStartTime() == null
                || now.isBefore(exam.getStartTime())
                || (effectiveEnd != null && now.isAfter(effectiveEnd))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Exam is not live");
        }
        if (!exam.isWebcamMonitoring()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Webcam monitoring is not enabled");
        }
    }
}
