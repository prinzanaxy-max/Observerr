package com.backend.observerr.exam.service;

import com.backend.observerr.exam.model.ExamStatus;
import com.backend.observerr.exam.repository.ExamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExamStartScheduler {

    private final ExamRepository examRepository;
    private final ExamLifecycleService examLifecycleService;

    @Scheduled(fixedRateString = "${exam.start-check-interval-ms:60000}")
    public void activateDueExams() {
        var dueExams = examRepository.findByStatusAndStartTimeLessThanEqualAndStartNotificationsSentFalse(
                ExamStatus.SCHEDULED,
                Instant.now()
        );

        if (dueExams.isEmpty()) {
            return;
        }

        log.info("Found {} exam(s) due to go LIVE", dueExams.size());
        for (var exam : dueExams) {
            try {
                examLifecycleService.transitionExamToLive(exam.getId());
            } catch (Exception ex) {
                log.error("Failed LIVE transition for examId={}: {}", exam.getId(), ex.getMessage(), ex);
            }
        }
    }
}
