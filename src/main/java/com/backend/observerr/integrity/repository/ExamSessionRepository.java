package com.backend.observerr.integrity.repository;

import com.backend.observerr.integrity.model.ExamSession;
import com.backend.observerr.integrity.model.ExamSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ExamSessionRepository extends JpaRepository<ExamSession, UUID> {

    Optional<ExamSession> findByIdAndStudentId(UUID id, Long studentId);

    @Query("""
            SELECT es FROM ExamSession es
            JOIN Exam e ON e.id = es.examId
            WHERE es.id = :sessionId AND e.lecturerId = :lecturerId
            """)
    Optional<ExamSession> findByIdAndLecturerId(@Param("sessionId") UUID sessionId, @Param("lecturerId") Long lecturerId);

    boolean existsByExamIdAndStudentIdAndStatus(Long examId, Long studentId, ExamSessionStatus status);
}
