package com.backend.observerr.exam.repository;

import com.backend.observerr.exam.model.ExamStudentBlock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExamStudentBlockRepository extends JpaRepository<ExamStudentBlock, Long> {
    Optional<ExamStudentBlock> findByExamIdAndStudentId(Long examId, Long studentId);
    boolean existsByExamIdAndStudentIdAndUnblockedAtIsNull(Long examId, Long studentId);
}
