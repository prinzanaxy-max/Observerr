package com.backend.observerr.student.results;

import com.backend.observerr.auth.model.User;
import com.backend.observerr.student.results.dto.CompletedAssessmentsPageDto;
import com.backend.observerr.student.results.dto.CompletedAssessmentsSummaryDto;
import com.backend.observerr.student.results.model.ResultsSortOption;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student/results")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentResultsController {

    private final StudentResultsService studentResultsService;

    @GetMapping
    public ResponseEntity<CompletedAssessmentsPageDto> getCompletedAssessments(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "MOST_RECENT") ResultsSortOption sort) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        return ResponseEntity.ok(studentResultsService.getCompletedAssessments(user, safePage, safeSize, sort));
    }

    @GetMapping("/summary")
    public ResponseEntity<CompletedAssessmentsSummaryDto> getSummary(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(studentResultsService.getSummary(user));
    }
}
