package com.backend.observerr.integrity.repository;

import com.backend.observerr.integrity.model.IntegrityEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IntegrityEventRepository extends JpaRepository<IntegrityEvent, Long> {

    boolean existsByClientEventId(UUID clientEventId);

    long countBySessionIdAndEventCode(UUID sessionId, String eventCode);

    List<IntegrityEvent> findBySessionIdOrderByOccurredAtAscIdAsc(UUID sessionId);
}
