package com.backend.observerr.integrity;

import com.backend.observerr.auth.model.User;
import com.backend.observerr.exam.model.Exam;
import com.backend.observerr.exam.model.ExamStatus;
import com.backend.observerr.exam.repository.ExamEnrollmentRepository;
import com.backend.observerr.exam.repository.ExamRepository;
import com.backend.observerr.exam.service.ExamStudentBlockService;
import com.backend.observerr.notification.NotificationService;
import com.backend.observerr.integrity.dto.*;
import com.backend.observerr.integrity.model.ExamSession;
import com.backend.observerr.integrity.model.ExamSessionStatus;
import com.backend.observerr.integrity.model.IntegrityEvent;
import com.backend.observerr.integrity.repository.ExamSessionRepository;
import com.backend.observerr.integrity.repository.IntegrityEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.cache.annotation.CacheEvict;
import com.backend.observerr.config.CacheConfig;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IntegritySessionService {

    private final ExamRepository examRepository;
    private final ExamEnrollmentRepository examEnrollmentRepository;
    private final ExamSessionRepository examSessionRepository;
    private final IntegrityEventRepository integrityEventRepository;
    private final IntegrityScoringPolicy scoringPolicy;
    private final ExamStudentBlockService blockService;
    private final NotificationService notificationService;

    @Transactional
    public ExamSessionResponse startSession(User student, Long examId, StartExamSessionRequest request) {
        Exam exam = examRepository.findByIdForUpdate(examId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam not found"));

        if (!exam.isPublished()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam not found");
        }
        requireStudentCanStart(student, exam);

        var existing = examSessionRepository.findByExamIdAndStudentIdAndStatus(
                examId, student.getId(), ExamSessionStatus.IN_PROGRESS);
        if (existing.isPresent()) {
            ExamSession session = existing.get();
            return ExamSessionResponse.builder()
                    .sessionId(session.getId().toString())
                    .examId(session.getExamId())
                    .studentId(session.getStudentId())
                    .startedAt(session.getStartedAt().toString())
                    .startingScore(session.getStartingScore())
                    .status(ExamSessionStatus.IN_PROGRESS.name())
                    .build();
        }

        UUID sessionId = UUID.randomUUID();
        Instant now = Instant.now();
        short startingScore = 100;

        ExamSession session = ExamSession.builder()
                .id(sessionId)
                .examId(examId)
                .studentId(student.getId())
                .startedAt(now)
                .startingScore(startingScore)
                .status(ExamSessionStatus.IN_PROGRESS)
                .proctoringAvailable(true)
                .build();

        examSessionRepository.save(session);

        return ExamSessionResponse.builder()
                .sessionId(sessionId.toString())
                .examId(examId)
                .studentId(student.getId())
                .startedAt(now.toString())
                .startingScore(startingScore)
                .status(ExamSessionStatus.IN_PROGRESS.name())
                .build();
    }

    @Transactional
    @CacheEvict(value = CacheConfig.LECTURER_ANALYTICS_OVERVIEW_CACHE, allEntries = true)
    public IntegrityEventsBatchResponse appendEvents(User student, UUID sessionId, IntegrityEventsBatchRequest request) {
        ExamSession session = loadStudentSessionForUpdate(student, sessionId);
        ensureInProgress(session);

        int accepted = 0;
        int skipped = 0;
        int deductions = session.getTotalDeductions();
        List<UUID> requestedIds = request.getEvents().stream()
                .map(IntegrityEventIngestDto::getClientEventId)
                .toList();
        Set<UUID> seenIds = new HashSet<>(integrityEventRepository.findExistingClientEventIds(requestedIds));
        List<IntegrityEvent> pendingEvents = new ArrayList<>();

        for (IntegrityEventIngestDto eventDto : request.getEvents()) {
            if (!seenIds.add(eventDto.getClientEventId())) {
                skipped++;
                continue;
            }
            IntegrityScoringPolicy.Rule rule = resolveRule(session, eventDto, deductions);
            deductions += rule.points();
            boolean eventKeepsProctoring = eventKeepsProctoring(eventDto.getEventCode());
            int currentScore = scoreFor(
                    session,
                    deductions,
                    session.isProctoringAvailable() && eventKeepsProctoring);
            pendingEvents.add(toEntity(sessionId, eventDto, rule, currentScore));
            notifyHighRisk(session, rule);
            if (!eventKeepsProctoring) {
                session.setProctoringAvailable(false);
            }
            session.setRequiresReview(session.isRequiresReview() || rule.requiresReview());
            accepted++;
        }

        if (accepted > 0) {
            integrityEventRepository.saveAll(pendingEvents);
            session.setTotalEvents(session.getTotalEvents() + accepted);
            session.setTotalDeductions(deductions);
            session.setFinalScore((short) scoreFor(session, deductions, session.isProctoringAvailable()));
            if (!session.isProctoringAvailable()) {
                session.setRequiresReview(true);
            }
            examSessionRepository.save(session);
        }

        return IntegrityEventsBatchResponse.builder()
                .accepted(accepted)
                .skipped(skipped)
                .currentScore(scoreFor(session, session.getTotalDeductions(), session.isProctoringAvailable()))
                .requiresReview(session.isRequiresReview())
                .build();
    }

    @Transactional
    @CacheEvict(value = CacheConfig.LECTURER_ANALYTICS_OVERVIEW_CACHE, allEntries = true)
    public CompleteExamSessionResponse completeSession(
            User student,
            UUID sessionId,
            CompleteExamSessionRequest request) {
        ExamSession session = loadStudentSessionForUpdate(student, sessionId);

        if (!session.getId().toString().equals(request.getSummary().getSessionId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Summary sessionId mismatch");
        }

        if (session.getStatus() == ExamSessionStatus.COMPLETED) {
            return CompleteExamSessionResponse.builder()
                    .sessionId(session.getId().toString())
                    .finalScore(session.getFinalScore() != null ? session.getFinalScore() : 0)
                    .requiresReview(session.isRequiresReview())
                    .status(ExamSessionStatus.COMPLETED.name())
                    .build();
        }

        if (request.getEvents() != null && !request.getEvents().isEmpty()) {
            ingestEvents(session, request.getEvents());
            session = loadStudentSession(student, sessionId);
        }

        ExamSessionSummaryDto summary = request.getSummary();
        if (!session.getExamId().equals(summary.getExamId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Summary examId mismatch");
        }
        boolean proctoringAvailable = session.isProctoringAvailable() && summary.isProctoringAvailable();
        int canonicalScore = scoreFor(session, session.getTotalDeductions(), proctoringAvailable);
        boolean canonicalReview = session.isRequiresReview() || !proctoringAvailable;
        validateSummary(session, summary);

        session.setEndedAt(parseInstant(summary.getEndedAt()));
        session.setFinalScore((short) canonicalScore);
        session.setRequiresReview(canonicalReview);
        session.setProctoringAvailable(proctoringAvailable);
        session.setStatus(ExamSessionStatus.COMPLETED);
        examSessionRepository.save(session);

        return CompleteExamSessionResponse.builder()
                .sessionId(session.getId().toString())
                .finalScore(canonicalScore)
                .requiresReview(canonicalReview)
                .status(ExamSessionStatus.COMPLETED.name())
                .build();
    }

    @Transactional(readOnly = true)
    public ExamSession getSessionForLecturer(User lecturer, UUID sessionId) {
        return examSessionRepository.findByIdAndLecturerId(sessionId, lecturer.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
    }

    @Transactional(readOnly = true)
    public List<IntegrityEvent> getSessionEvents(UUID sessionId) {
        return integrityEventRepository.findBySessionIdOrderByOccurredAtAscIdAsc(sessionId);
    }

    private ExamSession loadStudentSession(User student, UUID sessionId) {
        return examSessionRepository.findByIdAndStudentId(sessionId, student.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
    }

    private ExamSession loadStudentSessionForUpdate(User student, UUID sessionId) {
        ExamSession session = examSessionRepository.findByIdAndStudentIdForUpdate(sessionId, student.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
        blockService.requireNotBlocked(session.getExamId(), student.getId());
        return session;
    }

    private void ensureInProgress(ExamSession session) {
        if (session.getStatus() == ExamSessionStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Session is already completed");
        }
    }

    private void ingestEvents(ExamSession session, List<IntegrityEventIngestDto> events) {
        int accepted = 0;
        int deductions = session.getTotalDeductions();
        List<UUID> requestedIds = events.stream().map(IntegrityEventIngestDto::getClientEventId).toList();
        Set<UUID> seenIds = new HashSet<>(integrityEventRepository.findExistingClientEventIds(requestedIds));
        List<IntegrityEvent> pendingEvents = new ArrayList<>();
        for (IntegrityEventIngestDto eventDto : events) {
            if (!seenIds.add(eventDto.getClientEventId())) {
                continue;
            }
            IntegrityScoringPolicy.Rule rule = resolveRule(session, eventDto, deductions);
            deductions += rule.points();
            boolean eventKeepsProctoring = eventKeepsProctoring(eventDto.getEventCode());
            pendingEvents.add(toEntity(
                    session.getId(),
                    eventDto,
                    rule,
                    scoreFor(session, deductions, session.isProctoringAvailable() && eventKeepsProctoring)));
            notifyHighRisk(session, rule);
            if (!eventKeepsProctoring) {
                session.setProctoringAvailable(false);
            }
            session.setRequiresReview(session.isRequiresReview() || rule.requiresReview());
            accepted++;
        }
        if (accepted > 0) {
            integrityEventRepository.saveAll(pendingEvents);
            session.setTotalEvents(session.getTotalEvents() + accepted);
            session.setTotalDeductions(deductions);
            session.setFinalScore((short) scoreFor(session, deductions, session.isProctoringAvailable()));
            if (!session.isProctoringAvailable()) {
                session.setRequiresReview(true);
            }
            examSessionRepository.save(session);
        }
    }

    private IntegrityEvent toEntity(
            UUID sessionId,
            IntegrityEventIngestDto dto,
            IntegrityScoringPolicy.Rule rule,
            int scoreAfter) {
        return IntegrityEvent.builder()
                .sessionId(sessionId)
                .clientEventId(dto.getClientEventId())
                .eventCode(dto.getEventCode())
                .title(rule.title())
                .description(dto.getDescription())
                .severity(rule.severity())
                .pointsDeducted(rule.points())
                .scoreAfter(scoreAfter)
                .requiresReview(rule.requiresReview())
                .occurredAt(parseInstant(dto.getTimestamp()))
                .durationMs(dto.getDurationMs())
                .metadata(dto.getMetadata())
                .build();
    }

    private IntegrityScoringPolicy.Rule resolveRule(
            ExamSession session,
            IntegrityEventIngestDto dto,
            int deductionsBeforeEvent) {
        IntegrityScoringPolicy.Rule rule = scoringPolicy.resolve(dto.getEventCode(), dto.getDurationMs());
        if ("PROCTORING_UNAVAILABLE".equals(dto.getEventCode())) {
            int currentScore = Math.max(0, session.getStartingScore() - deductionsBeforeEvent);
            return new IntegrityScoringPolicy.Rule(
                    rule.title(),
                    rule.severity(),
                    Math.max(0, currentScore - IntegrityScoringPolicy.UNAVAILABLE_PROCTORING_SCORE_CAP),
                    true);
        }
        if (!"TAB_BLUR_REPEATED".equals(dto.getEventCode())) {
            return rule;
        }
        long tabBlurs = integrityEventRepository.countBySessionIdAndEventCode(session.getId(), "TAB_BLUR");
        long repeatedPenalties = integrityEventRepository.countBySessionIdAndEventCode(
                session.getId(),
                "TAB_BLUR_REPEATED");
        // Apply a streak penalty every 3 tab blurs (3rd, 6th, 9th, …).
        long expectedRepeated = tabBlurs / 3;
        if (tabBlurs >= 3 && repeatedPenalties < expectedRepeated) {
            return rule;
        }
        return new IntegrityScoringPolicy.Rule(
                "Repeated tab/window blur (already accounted for)",
                "INFO",
                0,
                false);
    }

    private boolean eventKeepsProctoring(String eventCode) {
        return !"CAMERA_PERMISSION_LOST".equals(eventCode)
                && !"PROCTORING_UNAVAILABLE".equals(eventCode);
    }

    private void notifyHighRisk(ExamSession session, IntegrityScoringPolicy.Rule rule) {
        if (!rule.requiresReview() && !"HIGH".equalsIgnoreCase(rule.severity())
                && !"CRITICAL".equalsIgnoreCase(rule.severity())) {
            return;
        }
        examRepository.findById(session.getExamId()).ifPresent(exam ->
                notificationService.sendHighRiskAlert(
                        exam.getLecturerId(), exam.getId(), rule.title()));
    }

    private void requireStudentCanStart(User student, Exam exam) {
        blockService.requireNotBlocked(exam.getId(), student.getId());
        if (!examEnrollmentRepository.existsByExamIdAndStudentId(exam.getId(), student.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Student is not enrolled in this exam");
        }
        Instant now = Instant.now();
        Instant effectiveEnd = exam.getEndTime();
        if (effectiveEnd == null && exam.getDurationMinutes() != null && exam.getStartTime() != null) {
            effectiveEnd = exam.getStartTime().plusSeconds(exam.getDurationMinutes() * 60L);
        }
        if (exam.getStatus() != ExamStatus.LIVE
                || exam.getStartTime() == null
                || now.isBefore(exam.getStartTime())
                || (effectiveEnd != null && now.isAfter(effectiveEnd))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Exam is not live");
        }
        if (!exam.isWebcamMonitoring()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Webcam monitoring is not enabled");
        }
    }

    private void validateSummary(ExamSession session, ExamSessionSummaryDto summary) {
        // Server totals are authoritative. Client summary may lag after refresh or
        // failed event flushes — reject only hard identity mismatches.
        if (summary.getStartingScore() != session.getStartingScore()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Completion summary starting score does not match session");
        }
    }

    private int scoreFor(ExamSession session, int deductions, boolean proctoringAvailable) {
        int score = Math.max(0, session.getStartingScore() - deductions);
        return proctoringAvailable
                ? score
                : Math.min(score, IntegrityScoringPolicy.UNAVAILABLE_PROCTORING_SCORE_CAP);
    }

    private Instant parseInstant(String value) {
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid timestamp format");
        }
    }

    public int computeDurationMinutes(ExamSession session) {
        Instant end = session.getEndedAt() != null ? session.getEndedAt() : Instant.now();
        return (int) Math.max(1, Duration.between(session.getStartedAt(), end).toMinutes());
    }
}
