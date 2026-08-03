package com.backend.observerr.integrity.repository;

import com.backend.observerr.integrity.model.IntegrityEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;
// List already imported for sumPoints query
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.backend.observerr.lecturer.analytics.repository.IntegrityReportRow;

public interface IntegrityEventRepository extends JpaRepository<IntegrityEvent, Long> {

    boolean existsByClientEventId(UUID clientEventId);

    @Query("SELECT event.clientEventId FROM IntegrityEvent event WHERE event.clientEventId IN :ids")
    List<UUID> findExistingClientEventIds(@Param("ids") List<UUID> ids);

    long countBySessionIdAndEventCode(UUID sessionId, String eventCode);

    @Query("""
            SELECT COALESCE(SUM(e.pointsDeducted), 0)
            FROM IntegrityEvent e
            WHERE e.sessionId = :sessionId AND e.eventCode IN :codes
            """)
    int sumPointsBySessionIdAndEventCodeIn(
            @Param("sessionId") UUID sessionId,
            @Param("codes") List<String> codes);

    List<IntegrityEvent> findBySessionIdOrderByOccurredAtAscIdAsc(UUID sessionId);

    @Query(value = """
            SELECT ie.id AS id, ie.session_id AS sessionId, es.student_id AS studentId,
                   TRIM(CONCAT(COALESCE(u.first_name, ''), ' ', COALESCE(u.last_name, ''))) AS studentName,
                   e.id AS examId, e.title AS examTitle, ie.event_code AS eventType,
                   ie.severity AS severity, ie.occurred_at AS occurredAt,
                   ie.points_deducted AS pointsDeducted
            FROM integrity_events ie
            JOIN exam_sessions es ON es.id = ie.session_id
            JOIN exams e ON e.id = es.exam_id
            JOIN users u ON u.id = es.student_id
            WHERE e.lecturer_id = :lecturerId
              AND ie.occurred_at >= :start
              AND (:eventType IS NULL OR ie.event_code = :eventType)
              AND (:severity IS NULL OR ie.severity = :severity
                   OR (:severity = 'DANGER' AND ie.severity IN ('HIGH', 'CRITICAL'))
                   OR (:severity = 'WARNING' AND ie.severity = 'MEDIUM')
                   OR (:severity = 'NEUTRAL' AND ie.severity IN ('LOW', 'INFO')))
              AND (:search IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(u.institutional_id) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(CONCAT(COALESCE(u.first_name, ''), ' ', COALESCE(u.last_name, '')))
                      LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY ie.occurred_at DESC, ie.id DESC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM integrity_events ie
            JOIN exam_sessions es ON es.id = ie.session_id
            JOIN exams e ON e.id = es.exam_id
            JOIN users u ON u.id = es.student_id
            WHERE e.lecturer_id = :lecturerId
              AND ie.occurred_at >= :start
              AND (:eventType IS NULL OR ie.event_code = :eventType)
              AND (:severity IS NULL OR ie.severity = :severity
                   OR (:severity = 'DANGER' AND ie.severity IN ('HIGH', 'CRITICAL'))
                   OR (:severity = 'WARNING' AND ie.severity = 'MEDIUM')
                   OR (:severity = 'NEUTRAL' AND ie.severity IN ('LOW', 'INFO')))
              AND (:search IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(u.institutional_id) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(CONCAT(COALESCE(u.first_name, ''), ' ', COALESCE(u.last_name, '')))
                      LIKE LOWER(CONCAT('%', :search, '%')))
            """, nativeQuery = true)
    Page<IntegrityReportRow> findReport(
            @Param("lecturerId") Long lecturerId,
            @Param("start") Instant start,
            @Param("search") String search,
            @Param("eventType") String eventType,
            @Param("severity") String severity,
            Pageable pageable);

    @Query(value = """
            SELECT DISTINCT ie.event_code
            FROM integrity_events ie
            JOIN exam_sessions es ON es.id = ie.session_id
            JOIN exams e ON e.id = es.exam_id
            WHERE e.lecturer_id = :lecturerId AND ie.occurred_at >= :start
            ORDER BY ie.event_code
            """, nativeQuery = true)
    List<String> findReportEventTypes(@Param("lecturerId") Long lecturerId, @Param("start") Instant start);
}
