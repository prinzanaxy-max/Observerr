package com.backend.observerr.integrity;

import com.backend.observerr.auth.model.User;
import com.backend.observerr.exam.model.Exam;
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
    private final ExamSessionRepository examSessionRepository;
    private final IntegrityEventRepository integrityEventRepository;

    @Transactional
    public ExamSessionResponse startSession(User student, Long examId, StartExamSessionRequest request) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam not found"));

        if (examSessionRepository.existsByExamIdAndStudentIdAndStatus(
                examId, student.getId(), ExamSessionStatus.IN_PROGRESS)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An exam session is already in progress");
        }

        UUID sessionId = UUID.randomUUID();
        Instant now = Instant.now();
        short startingScore = (short) request.getStartingScore();

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
        ExamSession session = loadStudentSession(student, sessionId);
        ensureInProgress(session);

        int accepted = 0;
        int skipped = 0;

        for (IntegrityEventIngestDto eventDto : request.getEvents()) {
            if (integrityEventRepository.existsByClientEventId(eventDto.getClientEventId())) {
                skipped++;
                continue;
            }
            integrityEventRepository.save(toEntity(sessionId, eventDto));
            accepted++;
        }

        if (accepted > 0) {
            session.setTotalEvents(session.getTotalEvents() + accepted);
            examSessionRepository.save(session);
        }

        return IntegrityEventsBatchResponse.builder()
                .accepted(accepted)
                .skipped(skipped)
                .build();
    }

    @Transactional
    public CompleteExamSessionResponse completeSession(
            User student,
            UUID sessionId,
            CompleteExamSessionRequest request) {
        ExamSession session = loadStudentSession(student, sessionId);

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
        session.setEndedAt(parseInstant(summary.getEndedAt()));
        session.setFinalScore((short) summary.getFinalScore());
        session.setTotalDeductions(summary.getTotalDeductions());
        session.setTotalEvents(summary.getTotalEvents());
        session.setRequiresReview(summary.isRequiresReview());
        session.setProctoringAvailable(summary.isProctoringAvailable());
        session.setStatus(ExamSessionStatus.COMPLETED);
        examSessionRepository.save(session);

        return CompleteExamSessionResponse.builder()
                .sessionId(session.getId().toString())
                .finalScore(summary.getFinalScore())
                .requiresReview(summary.isRequiresReview())
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

    private void ensureInProgress(ExamSession session) {
        if (session.getStatus() == ExamSessionStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Session is already completed");
        }
    }

    private void ingestEvents(ExamSession session, List<IntegrityEventIngestDto> events) {
        int accepted = 0;
        for (IntegrityEventIngestDto eventDto : events) {
            if (integrityEventRepository.existsByClientEventId(eventDto.getClientEventId())) {
                continue;
            }
            integrityEventRepository.save(toEntity(session.getId(), eventDto));
            accepted++;
        }
        if (accepted > 0) {
            session.setTotalEvents(session.getTotalEvents() + accepted);
            examSessionRepository.save(session);
        }
    }

    private IntegrityEvent toEntity(UUID sessionId, IntegrityEventIngestDto dto) {
        return IntegrityEvent.builder()
                .sessionId(sessionId)
                .clientEventId(dto.getClientEventId())
                .eventCode(dto.getEventCode())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .severity(dto.getSeverity())
                .pointsDeducted(dto.getPointsDeducted())
                .scoreAfter(dto.getScoreAfter())
                .requiresReview(dto.isRequiresReview())
                .occurredAt(parseInstant(dto.getTimestamp()))
                .durationMs(dto.getDurationMs())
                .metadata(dto.getMetadata())
                .build();
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
