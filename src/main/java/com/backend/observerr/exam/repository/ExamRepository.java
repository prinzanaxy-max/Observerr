package com.backend.observerr.exam.repository;

import com.backend.observerr.exam.model.Exam;
import com.backend.observerr.exam.model.ExamStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ExamRepository extends JpaRepository<Exam, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Exam e WHERE e.id = :examId")
    Optional<Exam> findByIdForUpdate(@Param("examId") Long examId);

    List<Exam> findByStatusAndStartTimeLessThanEqualAndStartNotificationsSentFalse(
            ExamStatus status,
            Instant startTime
    );

    List<Exam> findByStatusAndEndTimeLessThanEqual(ExamStatus status, Instant endTime);

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

    @Query(
            value = """
                    SELECT e FROM Exam e
                    JOIN ExamEnrollment en ON en.examId = e.id
                    WHERE en.studentId = :studentId AND e.published = true
                    ORDER BY CASE
                        WHEN e.startTime <= :now AND (e.endTime IS NULL OR e.endTime > :now) THEN 0
                        WHEN e.startTime > :now THEN 1
                        ELSE 2
                    END, e.startTime DESC
                    """,
            countQuery = """
                    SELECT COUNT(e) FROM Exam e
                    JOIN ExamEnrollment en ON en.examId = e.id
                    WHERE en.studentId = :studentId AND e.published = true
                    """)
    Page<Exam> findPublishedExamsForStudent(
            @Param("studentId") Long studentId,
            @Param("now") Instant now,
            Pageable pageable);

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

    @Query("""
            SELECT DISTINCT CONCAT(COALESCE(e.courseCode, ''), ':', COALESCE(e.courseName, ''))
            FROM Exam e WHERE e.lecturerId = :lecturerId
            ORDER BY CONCAT(COALESCE(e.courseCode, ''), ':', COALESCE(e.courseName, ''))
            """)
    List<String> findCourseLabelsByLecturerId(@Param("lecturerId") Long lecturerId);
}
