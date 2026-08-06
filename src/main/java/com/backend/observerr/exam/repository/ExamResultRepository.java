package com.backend.observerr.exam.repository;

import com.backend.observerr.exam.model.ExamResult;
import com.backend.observerr.exam.model.ExamResultStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExamResultRepository extends JpaRepository<ExamResult, Long> {

    Optional<ExamResult> findBySessionId(UUID sessionId);

    boolean existsByExamIdAndStudentId(Long examId, Long studentId);

    List<ExamResult> findByExamIdOrderBySubmittedAtDesc(Long examId);

    List<ExamResult> findByExamIdAndIdIn(Long examId, List<Long> ids);

    Page<ExamResult> findByStudentIdAndReleaseStatus(
            Long studentId, ExamResultStatus releaseStatus, Pageable pageable);

    Optional<ExamResult> findByIdAndStudentIdAndReleaseStatus(
            Long id, Long studentId, ExamResultStatus releaseStatus);

    long countByStudentIdAndReleaseStatus(Long studentId, ExamResultStatus releaseStatus);

    @Query("""
            SELECT COUNT(r) AS examsCompleted,
                   AVG(r.integrityScore) AS averageIntegrity,
                   COALESCE(SUM(CASE WHEN r.requiresReview = false THEN 1 ELSE 0 END), 0) AS verifiedSessions,
                   COALESCE(SUM(CASE WHEN r.requiresReview = true THEN 1 ELSE 0 END), 0) AS underReview
            FROM ExamResult r
            WHERE r.studentId = :studentId
            """)
    ExamResultStatsProjection summarizeStudent(@Param("studentId") Long studentId);
}
