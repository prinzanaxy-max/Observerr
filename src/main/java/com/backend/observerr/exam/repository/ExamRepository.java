package com.backend.observerr.exam.repository;

import com.backend.observerr.exam.model.Exam;
import com.backend.observerr.exam.model.ExamStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface ExamRepository extends JpaRepository<Exam, Long> {

    List<Exam> findByStatusAndStartTimeLessThanEqualAndStartNotificationsSentFalse(
            ExamStatus status,
            Instant startTime
    );
}
