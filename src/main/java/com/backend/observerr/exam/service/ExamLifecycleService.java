package com.backend.observerr.exam.service;

import com.backend.observerr.exam.ExamDisplayStatusResolver;
import com.backend.observerr.exam.model.Exam;
import com.backend.observerr.exam.model.ExamStatus;
import com.backend.observerr.exam.repository.ExamRepository;
import com.backend.observerr.notification.NotificationService;
import com.backend.observerr.integrity.model.ExamSession;
import com.backend.observerr.integrity.model.ExamSessionStatus;
import com.backend.observerr.integrity.repository.ExamSessionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExamLifecycleService {

    private final ExamRepository examRepository;
    private final NotificationService notificationService;
    private final ExamSessionRepository sessionRepository;

    /**
     * Idempotent LIVE transition:
     * - Only moves SCHEDULED -> LIVE
     * - Uses startNotificationsSent to prevent duplicate pushes if scheduler re-runs
     */
    @Transactional
    public void transitionExamToLive(Long examId) {
        Exam exam = examRepository.findByIdForUpdate(examId)
                .orElseThrow(() -> new EntityNotFoundException("Exam not found: " + examId));

        if (exam.isStartNotificationsSent()) {
            log.debug("Start notifications already sent for examId={} — skipping", examId);
            return;
        }

        if (exam.getStatus() != ExamStatus.SCHEDULED) {
            log.debug("Exam examId={} status={} is not SCHEDULED — skipping LIVE transition",
                    examId, exam.getStatus());
            return;
        }
        if (!exam.isPublished()) {
            log.debug("Exam examId={} is not published — skipping LIVE transition", examId);
            return;
        }

        Instant now = Instant.now();
        // Manual / early Go Live: align the window so students can enter immediately.
        if (exam.getStartTime() != null && now.isBefore(exam.getStartTime())) {
            int durationMinutes = ExamDisplayStatusResolver.resolveDurationMinutes(exam);
            exam.setStartTime(now);
            exam.setEndTime(now.plusSeconds(durationMinutes * 60L));
        }

        exam.setStatus(ExamStatus.LIVE);
        examRepository.save(exam);

        notificationService.notifyStudentsExamStarted(examId);
        notificationService.notifyLecturerExamStarted(examId);

        exam.setStartNotificationsSent(true);
        examRepository.save(exam);

        log.info("Exam transitioned to LIVE examId={} title='{}'", examId, exam.getTitle());
    }

    @Transactional
    public void endExam(Long examId) {
        Exam exam = examRepository.findByIdForUpdate(examId)
                .orElseThrow(() -> new EntityNotFoundException("Exam not found: " + examId));
        if (exam.getStatus() == ExamStatus.ENDED) {
            return;
        }
        if (exam.getStatus() != ExamStatus.LIVE) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT, "Only a live exam can be ended");
        }
        Instant now = Instant.now();
        for (ExamSession session : sessionRepository.findByExamId(examId)) {
            if (session.getStatus() == ExamSessionStatus.IN_PROGRESS) {
                session.setStatus(ExamSessionStatus.COMPLETED);
                session.setEndedAt(now);
                session.setFinalScore((short) Math.max(0,
                        session.getStartingScore() - session.getTotalDeductions()));
                session.setRequiresReview(true);
            }
        }
        exam.setStatus(ExamStatus.ENDED);
        exam.setEndTime(now);
        examRepository.save(exam);
        notificationService.notifyExamEnded(examId);
    }
}
