package com.backend.observerr.exam.controller;

import com.backend.observerr.auth.model.User;
import com.backend.observerr.exam.dto.*;
import com.backend.observerr.exam.service.ExamAttemptService;
import com.backend.observerr.exam.service.ExamQuestionService;
import com.backend.observerr.security.RequestRateLimiter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/student/exam-sessions/{sessionId}")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentExamAttemptController {

    private final ExamQuestionService questionService;
    private final ExamAttemptService attemptService;
    private final RequestRateLimiter requestRateLimiter;

    @GetMapping("/questions")
    public ResponseEntity<List<StudentQuestionDto>> getQuestions(
            @AuthenticationPrincipal User student,
            @PathVariable UUID sessionId) {
        return ResponseEntity.ok(questionService.getSessionQuestions(student, sessionId));
    }

    @GetMapping("/answers")
    public ResponseEntity<List<SavedAnswerDto>> restoreAnswers(
            @AuthenticationPrincipal User student,
            @PathVariable UUID sessionId) {
        return ResponseEntity.ok(attemptService.restoreAnswers(student, sessionId));
    }

    @PutMapping("/answers/{questionId}")
    public ResponseEntity<SavedAnswerDto> saveAnswer(
            @AuthenticationPrincipal User student,
            @PathVariable UUID sessionId,
            @PathVariable Long questionId,
            @Valid @RequestBody SaveAnswerRequest request) {
        requestRateLimiter.autosave(student.getId());
        return ResponseEntity.ok(attemptService.saveAnswer(student, sessionId, questionId, request));
    }

    @PostMapping("/submit")
    public ResponseEntity<ExamResultDto> submit(
            @AuthenticationPrincipal User student,
            @PathVariable UUID sessionId,
            @Valid @RequestBody SubmitExamRequest request) {
        return ResponseEntity.ok(attemptService.submit(student, sessionId, request));
    }
}
