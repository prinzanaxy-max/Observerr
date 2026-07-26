package com.backend.observerr.lecturer.analytics;

import com.backend.observerr.auth.model.User;
import com.backend.observerr.lecturer.analytics.dto.LecturerAnalyticsOverviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lecturer/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('LECTURER')")
public class LecturerAnalyticsController {

    private final LecturerAnalyticsService lecturerAnalyticsService;

    @GetMapping("/overview")
    public ResponseEntity<LecturerAnalyticsOverviewResponse> getOverview(
            @AuthenticationPrincipal User lecturer,
            @RequestParam(defaultValue = "7D") String period) {
        return ResponseEntity.ok(lecturerAnalyticsService.getOverview(lecturer, period));
    }
}
