package com.backend.observerr.exam.service;

import com.backend.observerr.exam.model.Exam;
import com.backend.observerr.exam.model.ExamStatus;
import com.backend.observerr.exam.repository.ExamRepository;
import com.backend.observerr.notification.NotificationService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExamLifecycleService {

    private final ExamRepository examRepository;
    private final NotificationService notificationService;

    /**
     * Idempotent LIVE transition:
     * - Only moves SCHEDULED -> LIVE
     * - Uses startNotificationsSent to prevent duplicate pushes if scheduler re-runs
     */
    @Transactional
    public void transitionExamToLive(Long examId) {
        Exam exam = examRepository.findById(examId)
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

        exam.setStatus(ExamStatus.LIVE);
        examRepository.save(exam);

        notificationService.notifyStudentsExamStarted(examId);
        notificationService.notifyLecturerExamStarted(examId);

        exam.setStartNotificationsSent(true);
        examRepository.save(exam);

        log.info("Exam transitioned to LIVE examId={} title='{}'", examId, exam.getTitle());
    }
}
