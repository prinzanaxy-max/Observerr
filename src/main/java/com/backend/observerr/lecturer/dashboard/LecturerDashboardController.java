package com.backend.observerr.lecturer.dashboard;

import com.backend.observerr.auth.model.User;
import com.backend.observerr.lecturer.dashboard.dto.LecturerDashboardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lecturer/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('LECTURER')")
public class LecturerDashboardController {

    private final LecturerDashboardService lecturerDashboardService;

    @GetMapping
    public ResponseEntity<LecturerDashboardResponse> getDashboard(@AuthenticationPrincipal User lecturer) {
        return ResponseEntity.ok(lecturerDashboardService.getDashboard(lecturer));
    }
}
