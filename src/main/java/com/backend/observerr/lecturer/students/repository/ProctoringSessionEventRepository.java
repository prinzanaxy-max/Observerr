package com.backend.observerr.lecturer.students.repository;

import com.backend.observerr.lecturer.students.model.ProctoringSessionEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProctoringSessionEventRepository extends JpaRepository<ProctoringSessionEvent, Long> {

    List<ProctoringSessionEvent> findBySessionIdOrderBySortOrderAsc(Long sessionId);
}
