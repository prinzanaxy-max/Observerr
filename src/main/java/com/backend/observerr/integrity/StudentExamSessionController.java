package com.backend.observerr.integrity;

import com.backend.observerr.auth.model.User;
import com.backend.observerr.integrity.dto.*;
import com.backend.observerr.proctoring.ProctoringMediaService;
import com.backend.observerr.proctoring.dto.LiveKitTokenResponse;
import com.backend.observerr.exam.service.ExamAttemptService;
import com.backend.observerr.security.RequestRateLimiter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class StudentExamSessionController {

    private final IntegritySessionService integritySessionService;
    private final ExamAttemptService examAttemptService;
    private final ProctoringMediaService proctoringMediaService;
    private final RequestRateLimiter requestRateLimiter;

    @PostMapping("/api/student/exams/{examId}/sessions")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ExamSessionResponse> startSession(
            @AuthenticationPrincipal User student,
            @PathVariable Long examId,
            @Valid @RequestBody(required = false) StartExamSessionRequest request) {
        StartExamSessionRequest body = request != null ? request : new StartExamSessionRequest();
        requestRateLimiter.session(student.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(integritySessionService.startSession(student, examId, body));
    }

    @PostMapping("/api/student/exam-sessions/{sessionId}/integrity-events")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<IntegrityEventsBatchResponse> appendIntegrityEvents(
            @AuthenticationPrincipal User student,
            @PathVariable UUID sessionId,
            @Valid @RequestBody IntegrityEventsBatchRequest request) {
        requestRateLimiter.integrity(student.getId());
        return ResponseEntity.ok(integritySessionService.appendEvents(student, sessionId, request));
    }

    @PostMapping("/api/student/exam-sessions/{sessionId}/media-token")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<LiveKitTokenResponse> issueMediaToken(
            @AuthenticationPrincipal User student,
            @PathVariable UUID sessionId) {
        return ResponseEntity.ok(proctoringMediaService.issueStudentToken(student, sessionId));
    }

    @PostMapping("/api/student/exam-sessions/{sessionId}/complete")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<CompleteExamSessionResponse> completeSession(
            @AuthenticationPrincipal User student,
            @PathVariable UUID sessionId,
            @Valid @RequestBody CompleteExamSessionRequest request) {
        return ResponseEntity.ok(examAttemptService.completeAndGrade(student, sessionId, request));
    }
}
