package com.backend.observerr.exam.repository;

import com.backend.observerr.exam.model.ExamQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExamQuestionRepository extends JpaRepository<ExamQuestion, Long> {

    List<ExamQuestion> findByExamIdOrderByDisplayOrderAsc(Long examId);

    boolean existsByExamId(Long examId);

    void deleteByExamId(Long examId);
}
