package com.backend.observerr.integrity;

import com.backend.observerr.auth.model.User;
import com.backend.observerr.exam.model.Exam;
import com.backend.observerr.exam.model.ExamStatus;
import com.backend.observerr.exam.repository.ExamEnrollmentRepository;
import com.backend.observerr.exam.repository.ExamRepository;
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

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IntegritySessionService {

    private final ExamRepository examRepository;
    private final ExamEnrollmentRepository examEnrollmentRepository;
    private final ExamSessionRepository examSessionRepository;
    private final IntegrityEventRepository integrityEventRepository;
    private final IntegrityScoringPolicy scoringPolicy;

    @Transactional
    public ExamSessionResponse startSession(User student, Long examId, StartExamSessionRequest request) {
        Exam exam = examRepository.findByIdForUpdate(examId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam not found"));

        if (!exam.isPublished()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam not found");
        }
        requireStudentCanStart(student, exam);

        if (examSessionRepository.existsByExamIdAndStudentIdAndStatus(
                examId, student.getId(), ExamSessionStatus.IN_PROGRESS)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An exam session is already in progress");
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
    public IntegrityEventsBatchResponse appendEvents(User student, UUID sessionId, IntegrityEventsBatchRequest request) {
        ExamSession session = loadStudentSessionForUpdate(student, sessionId);
        ensureInProgress(session);

        int accepted = 0;
        int skipped = 0;
        int deductions = session.getTotalDeductions();

        for (IntegrityEventIngestDto eventDto : request.getEvents()) {
            if (integrityEventRepository.existsByClientEventId(eventDto.getClientEventId())) {
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
            integrityEventRepository.save(toEntity(sessionId, eventDto, rule, currentScore));
            if (!eventKeepsProctoring) {
                session.setProctoringAvailable(false);
            }
            session.setRequiresReview(session.isRequiresReview() || rule.requiresReview());
            accepted++;
        }

        if (accepted > 0) {
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
        validateSummary(session, summary, canonicalScore, canonicalReview, proctoringAvailable);

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
        return examSessionRepository.findByIdAndStudentIdForUpdate(sessionId, student.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
    }

    private void ensureInProgress(ExamSession session) {
        if (session.getStatus() == ExamSessionStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Session is already completed");
        }
    }

    private void ingestEvents(ExamSession session, List<IntegrityEventIngestDto> events) {
        int accepted = 0;
        int deductions = session.getTotalDeductions();
        for (IntegrityEventIngestDto eventDto : events) {
            if (integrityEventRepository.existsByClientEventId(eventDto.getClientEventId())) {
                continue;
            }
            IntegrityScoringPolicy.Rule rule = resolveRule(session, eventDto, deductions);
            deductions += rule.points();
            boolean eventKeepsProctoring = eventKeepsProctoring(eventDto.getEventCode());
            integrityEventRepository.save(toEntity(
                    session.getId(),
                    eventDto,
                    rule,
                    scoreFor(session, deductions, session.isProctoringAvailable() && eventKeepsProctoring)));
            if (!eventKeepsProctoring) {
                session.setProctoringAvailable(false);
            }
            session.setRequiresReview(session.isRequiresReview() || rule.requiresReview());
            accepted++;
        }
        if (accepted > 0) {
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
        if (tabBlurs >= 3 && repeatedPenalties == 0) {
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

    private void requireStudentCanStart(User student, Exam exam) {
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

    private void validateSummary(
            ExamSession session,
            ExamSessionSummaryDto summary,
            int canonicalScore,
            boolean canonicalReview,
            boolean proctoringAvailable) {
        if (summary.getStartingScore() != session.getStartingScore()
                || summary.getTotalEvents() != session.getTotalEvents()
                || summary.getTotalDeductions() != session.getTotalDeductions()
                || summary.getFinalScore() != canonicalScore
                || summary.isRequiresReview() != canonicalReview
                || summary.isProctoringAvailable() != proctoringAvailable) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Completion summary does not match server integrity state");
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
