package com.backend.observerr.lecturer.analytics;

import com.backend.observerr.auth.model.User;
import com.backend.observerr.lecturer.analytics.dto.LecturerAnalyticsOverviewResponse;
import com.backend.observerr.lecturer.analytics.dto.IntegrityReportPageDto;
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
            @RequestParam(required = false, defaultValue = "7D") String period,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return ResponseEntity.ok(lecturerAnalyticsService.getOverview(lecturer, period, startDate, endDate));
    }

    @GetMapping("/integrity-events")
    public ResponseEntity<IntegrityReportPageDto> getIntegrityEvents(
            @AuthenticationPrincipal User lecturer,
            @RequestParam(required = false, defaultValue = "7D") String period,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String severity) {
        return ResponseEntity.ok(lecturerAnalyticsService.getIntegrityEvents(
                lecturer, period, startDate, endDate, page, size, search, eventType, severity));
    }
}
