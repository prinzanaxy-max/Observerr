package com.backend.observerr.lecturer.students.repository;

import com.backend.observerr.lecturer.students.model.ProctoringSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProctoringSessionRepository extends JpaRepository<ProctoringSession, Long> {

    Optional<ProctoringSession> findByIdAndLecturerId(Long id, Long lecturerId);
}
