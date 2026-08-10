package com.backend.observerr.exam.repository;

import com.backend.observerr.exam.model.ExamAnswer;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExamAnswerRepository extends JpaRepository<ExamAnswer, Long> {

    List<ExamAnswer> findBySessionIdOrderByQuestionIdAsc(UUID sessionId);

    Optional<ExamAnswer> findBySessionIdAndQuestionId(UUID sessionId, Long questionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM ExamAnswer a WHERE a.sessionId = :sessionId AND a.questionId = :questionId")
    Optional<ExamAnswer> findForUpdate(
            @Param("sessionId") UUID sessionId,
            @Param("questionId") Long questionId);
}
