package com.backend.observerr.lecturer.dashboard;

import com.backend.observerr.exam.dto.LecturerExamDto;
import com.backend.observerr.exam.model.Exam;
import com.backend.observerr.lecturer.analytics.dto.LecturerAnalyticsOverviewResponse;
import com.backend.observerr.lecturer.dashboard.dto.IntegrityTrendSummaryDto;
import com.backend.observerr.lecturer.dashboard.dto.LiveExamBannerDto;
import com.backend.observerr.lecturer.dashboard.dto.NeedsReviewItemDto;
import com.backend.observerr.lecturer.dashboard.dto.TopFlaggedBehaviorDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Slf4j
@Component
public class LecturerDashboardFallbacks {

    public LiveExamBannerDto liveExamBannerOrFallback(
            Supplier<LiveExamBannerDto> build,
            LecturerExamDto examDto) {
        try {
            return build.get();
        } catch (DataAccessException ex) {
            log.warn("Dashboard live exam session query failed, using exam metadata only: {}", ex.getMessage());
            return fallbackLiveExamBanner(examDto);
        } catch (RuntimeException ex) {
            log.warn("Dashboard live exam banner failed, using exam metadata only: {}", ex.getMessage());
            return fallbackLiveExamBanner(examDto);
        }
    }

    public List<NeedsReviewItemDto> needsReviewOrEmpty(Supplier<List<NeedsReviewItemDto>> build) {
        try {
            return build.get();
        } catch (DataAccessException ex) {
            log.warn("Dashboard needs-review query failed: {}", ex.getMessage());
            return List.of();
        }
    }

    public Optional<LecturerAnalyticsOverviewResponse> analyticsOrEmpty(
            Supplier<Optional<LecturerAnalyticsOverviewResponse>> load) {
        try {
            return load.get();
        } catch (DataAccessException ex) {
            log.warn("Dashboard analytics slice unavailable: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public LiveExamBannerDto fallbackLiveExamBanner(LecturerExamDto examDto) {
        return LiveExamBannerDto.builder()
                .examId(examDto.getId())
                .title(examDto.getTitle())
                .courseCode(examDto.getCourseCode())
                .status("LIVE")
                .remainingSeconds(0)
                .activeStudents(0)
                .highRiskCount(Math.max(0, examDto.getActiveFlagsCount()))
                .avgIntegrityScore(88.0)
                .liveMonitoringPath("/lecturer/exams/" + examDto.getId() + "/live")
                .build();
    }

    public IntegrityTrendSummaryDto emptyIntegrityTrend() {
        return IntegrityTrendSummaryDto.builder()
                .changeLabel("No trend data")
                .changeDirection("STABLE")
                .points(List.of())
                .build();
    }

    public List<TopFlaggedBehaviorDto> emptyBehaviors() {
        return List.of();
    }

    public static long remainingSeconds(Exam exam) {
        Instant end = exam.getEndTime();
        if (end == null && exam.getStartTime() != null && exam.getDurationMinutes() != null) {
            end = exam.getStartTime().plusSeconds(exam.getDurationMinutes() * 60L);
        }
        if (end == null) {
            return 0;
        }
        return Math.max(0, Duration.between(Instant.now(), end).getSeconds());
    }
}
