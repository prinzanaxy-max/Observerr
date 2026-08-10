package com.backend.observerr.lecturer.students.repository;

import com.backend.observerr.lecturer.students.model.ProctoringSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProctoringSessionRepository extends JpaRepository<ProctoringSession, Long> {

    Optional<ProctoringSession> findByIdAndLecturerId(Long id, Long lecturerId);

    @Query("""
            SELECT ps FROM ProctoringSession ps
            WHERE ps.lecturerId = :lecturerId AND ps.integrityScore < :threshold
            ORDER BY ps.sessionDate DESC, ps.id DESC
            """)
    List<ProctoringSession> findNeedsReviewForLecturer(
            @Param("lecturerId") Long lecturerId,
            @Param("threshold") int threshold);
}
