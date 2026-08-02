package com.backend.observerr.student.stats;

import com.backend.observerr.auth.model.User;
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

    private final StudentStatsService studentStatsService;

    @GetMapping
    public ResponseEntity<StudentStatsResponse> getStats(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(studentStatsService.getStats(user));
    }
}
