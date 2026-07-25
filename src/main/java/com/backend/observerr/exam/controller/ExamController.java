package com.backend.observerr.exam.controller;

import com.backend.observerr.exam.service.ExamLifecycleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/lecturer/exams")
@RequiredArgsConstructor
public class ExamController {

    private final ExamLifecycleService examLifecycleService;

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
