package com.backend.observerr.exam.repository;

import com.backend.observerr.exam.model.Exam;
import com.backend.observerr.exam.model.ExamStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ExamRepository extends JpaRepository<Exam, Long> {

    List<Exam> findByStatusAndStartTimeLessThanEqualAndStartNotificationsSentFalse(
            ExamStatus status,
            Instant startTime
    );

    List<Exam> findByLecturerIdAndPublishedTrueOrderByStartTimeDesc(Long lecturerId);

    List<Exam> findByLecturerIdOrderByStartTimeDesc(Long lecturerId);

    Optional<Exam> findByIdAndLecturerId(Long id, Long lecturerId);
}
