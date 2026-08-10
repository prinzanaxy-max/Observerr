package com.backend.observerr.student.results;

import com.backend.observerr.auth.model.User;
import com.backend.observerr.exam.dto.ExamResultDetailDto;
import com.backend.observerr.exam.dto.ExamResultsPageDto;
import com.backend.observerr.exam.service.ExamAttemptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student/results")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentResultsController {

    private final ExamAttemptService examAttemptService;

    @GetMapping
    public ResponseEntity<ExamResultsPageDto> getResults(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "MOST_RECENT") String sort) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        return ResponseEntity.ok(examAttemptService.studentResults(user, safePage, safeSize, sort));
    }

    @GetMapping("/{resultId}")
    public ResponseEntity<ExamResultDetailDto> getResult(
            @AuthenticationPrincipal User user,
            @PathVariable Long resultId) {
        return ResponseEntity.ok(examAttemptService.studentResultDetail(user, resultId));
    }

    @GetMapping("/{resultId}/analysis")
    public ResponseEntity<ExamResultDetailDto> getAnalysis(
            @AuthenticationPrincipal User user,
            @PathVariable Long resultId) {
        return ResponseEntity.ok(examAttemptService.studentResultDetail(user, resultId));
    }
}
