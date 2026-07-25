package com.backend.observerr.exam.controller;

import com.backend.observerr.auth.model.User;
import com.backend.observerr.exam.dto.CreateExamRequest;
import com.backend.observerr.exam.dto.LecturerExamDto;
import com.backend.observerr.exam.dto.LecturerExamListResponse;
import com.backend.observerr.exam.service.ExamLifecycleService;
import com.backend.observerr.exam.service.LecturerExamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/lecturer/exams")
@RequiredArgsConstructor
public class ExamController {

    private final ExamLifecycleService examLifecycleService;
    private final LecturerExamService lecturerExamService;

    @GetMapping
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<LecturerExamListResponse> listExams(
            @AuthenticationPrincipal User lecturer,
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(lecturerExamService.listExams(lecturer, status, search));
    }

    @GetMapping("/{examId}")
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<LecturerExamDto> getExam(
            @AuthenticationPrincipal User lecturer,
            @PathVariable Long examId) {
        return ResponseEntity.ok(lecturerExamService.getExam(lecturer, examId));
    }

    @PostMapping
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<LecturerExamDto> createExam(
            @AuthenticationPrincipal User lecturer,
            @Valid @RequestBody CreateExamRequest request) {
        LecturerExamDto created = lecturerExamService.createExam(lecturer, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/{examId}/start")
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<Map<String, Object>> startExam(@PathVariable Long examId) {
        examLifecycleService.transitionExamToLive(examId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Exam start triggered",
                "examId", examId
        ));
    }
}
