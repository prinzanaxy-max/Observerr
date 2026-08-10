package com.backend.observerr.student.results.repository;

import com.backend.observerr.student.results.model.AssessmentResultStatus;
import com.backend.observerr.student.results.model.StudentCompletedAssessment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentCompletedAssessmentRepository extends JpaRepository<StudentCompletedAssessment, Long> {

    Page<StudentCompletedAssessment> findByStudentId(Long studentId, Pageable pageable);

    @Query("""
            SELECT COALESCE(AVG(a.integrityScore), 0)
            FROM StudentCompletedAssessment a
            WHERE a.studentId = :studentId
            """)
    double averageIntegrityScoreByStudentId(@Param("studentId") Long studentId);

    long countByStudentId(Long studentId);

    long countByStudentIdAndStatus(Long studentId, AssessmentResultStatus status);
}
