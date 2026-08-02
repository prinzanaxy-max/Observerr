package com.backend.observerr.integrity.repository;

import com.backend.observerr.exam.model.Exam;
import com.backend.observerr.integrity.model.ExamSession;
import com.backend.observerr.integrity.model.ExamSessionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExamSessionRepository extends JpaRepository<ExamSession, UUID> {

    Optional<ExamSession> findByIdAndStudentId(UUID id, Long studentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT es FROM ExamSession es
            WHERE es.id = :sessionId AND es.studentId = :studentId
            """)
    Optional<ExamSession> findByIdAndStudentIdForUpdate(
            @Param("sessionId") UUID sessionId,
            @Param("studentId") Long studentId);

    @Query("""
            SELECT es FROM ExamSession es
            JOIN Exam e ON e.id = es.examId
            WHERE es.id = :sessionId AND e.lecturerId = :lecturerId
            """)
    Optional<ExamSession> findByIdAndLecturerId(@Param("sessionId") UUID sessionId, @Param("lecturerId") Long lecturerId);

    boolean existsByExamIdAndStudentIdAndStatus(Long examId, Long studentId, ExamSessionStatus status);

    List<ExamSession> findByExamId(Long examId);

    @Query("""
            SELECT es FROM ExamSession es
            JOIN Exam e ON e.id = es.examId
            WHERE es.examId = :examId AND e.lecturerId = :lecturerId
            ORDER BY es.startedAt DESC
            """)
    List<ExamSession> findByExamIdAndLecturerId(@Param("examId") Long examId, @Param("lecturerId") Long lecturerId);

    @Query("""
            SELECT es FROM ExamSession es
            JOIN Exam e ON e.id = es.examId
            WHERE e.lecturerId = :lecturerId
              AND (es.requiresReview = true
                   OR (es.finalScore IS NOT NULL AND es.finalScore < :threshold)
                   OR (es.status = :inProgressStatus
                       AND es.totalDeductions > 0 AND (es.startingScore - es.totalDeductions) < :threshold))
            ORDER BY es.updatedAt DESC
            """)
    List<ExamSession> findNeedsReviewForLecturer(
            @Param("lecturerId") Long lecturerId,
            @Param("threshold") int threshold,
            @Param("inProgressStatus") ExamSessionStatus inProgressStatus);
}
