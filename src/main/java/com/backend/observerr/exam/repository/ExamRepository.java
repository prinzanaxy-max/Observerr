package com.backend.observerr.exam.repository;

import com.backend.observerr.exam.model.Exam;
import com.backend.observerr.exam.model.ExamStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("""
            SELECT e FROM Exam e
            JOIN ExamEnrollment en ON en.examId = e.id
            WHERE en.studentId = :studentId AND e.published = true
            ORDER BY e.startTime DESC
            """)
    List<Exam> findPublishedExamsForStudent(@Param("studentId") Long studentId);

    List<Exam> findByPublishedTrueOrderByStartTimeDesc();

    Optional<Exam> findByIdAndPublishedTrue(Long id);

    @Query("""
            SELECT e FROM Exam e
            JOIN ExamEnrollment en ON en.examId = e.id
            WHERE e.id = :examId AND en.studentId = :studentId AND e.published = true
            """)
    Optional<Exam> findPublishedExamForStudent(
            @Param("examId") Long examId,
            @Param("studentId") Long studentId);
}
