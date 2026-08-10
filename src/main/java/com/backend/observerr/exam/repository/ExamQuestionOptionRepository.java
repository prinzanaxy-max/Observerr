package com.backend.observerr.exam.repository;

import com.backend.observerr.exam.model.ExamQuestionOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ExamQuestionOptionRepository extends JpaRepository<ExamQuestionOption, Long> {

    List<ExamQuestionOption> findByQuestionIdInOrderByQuestionIdAscOptionKeyAsc(Collection<Long> questionIds);

    List<ExamQuestionOption> findByQuestionIdOrderByOptionKeyAsc(Long questionId);

    void deleteByQuestionIdIn(Collection<Long> questionIds);
}
