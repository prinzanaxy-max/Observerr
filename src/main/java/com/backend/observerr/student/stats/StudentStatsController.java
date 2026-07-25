package com.backend.observerr.student.stats;

import com.backend.observerr.auth.model.User;
import com.backend.observerr.student.results.StudentResultsService;
import com.backend.observerr.student.results.dto.CompletedAssessmentsSummaryDto;
import com.backend.observerr.student.stats.dto.StudentStatsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student/stats")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentStatsController {

    private final StudentResultsService studentResultsService;

    @GetMapping
    public ResponseEntity<StudentStatsResponse> getStats(@AuthenticationPrincipal User user) {
        CompletedAssessmentsSummaryDto summary = studentResultsService.getSummary(user);
        return ResponseEntity.ok(StudentStatsResponse.builder()
                .examsCompleted(summary.getExamsCompleted())
                .avgIntegrity(summary.getAvgIntegrity())
                .verifiedSessions(summary.getVerifiedSessions())
                .underReview(summary.getUnderReview())
                .build());
    }
}
