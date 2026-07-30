package com.backend.observerr.exam.repository;

import com.backend.observerr.exam.model.ExamEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExamEnrollmentRepository extends JpaRepository<ExamEnrollment, Long> {

    @Query("SELECT e.studentId FROM ExamEnrollment e WHERE e.examId = :examId")
    List<Long> findStudentIdsByExamId(@Param("examId") Long examId);

    long countByExamId(Long examId);

    boolean existsByExamIdAndStudentId(Long examId, Long studentId);
}
