package com.backend.observerr.lecturer.analytics;

import com.backend.observerr.auth.model.User;
import com.backend.observerr.lecturer.analytics.dto.*;
import com.backend.observerr.lecturer.analytics.model.LecturerAnalyticsBehavior;
import com.backend.observerr.lecturer.analytics.model.LecturerAnalyticsOverview;
import com.backend.observerr.lecturer.analytics.model.LecturerAnalyticsTrendPoint;
import com.backend.observerr.lecturer.analytics.repository.LecturerAnalyticsOverviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class LecturerAnalyticsService {

    private static final Set<String> SUPPORTED_PERIODS = Set.of("7D", "30D", "3M");

    private final LecturerAnalyticsOverviewRepository overviewRepository;

    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = "lecturerAnalyticsOverview",
            key = "#lecturer.id + ':' + #period"
    )
    public LecturerAnalyticsOverviewResponse getOverview(User lecturer, String period) {
        String normalizedPeriod = normalizePeriod(period);
        LecturerAnalyticsOverview overview = overviewRepository
                .findByLecturerIdAndPeriod(lecturer.getId(), normalizedPeriod)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Analytics overview not found for period " + normalizedPeriod));

        overview.getTrendPoints().size();
        overview.getBehaviors().size();

        return toResponse(overview);
    }

    private String normalizePeriod(String period) {
        if (period == null || period.isBlank()) {
            return "7D";
        }
        String normalized = period.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_PERIODS.contains(normalized)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unsupported period. Use 7D, 30D, or 3M.");
        }
        return normalized;
    }

    private LecturerAnalyticsOverviewResponse toResponse(LecturerAnalyticsOverview overview) {
        return LecturerAnalyticsOverviewResponse.builder()
                .period(overview.getPeriod())
                .totalExamsMonitored(AnalyticsMetricCardDto.builder()
                        .value(overview.getTotalExamsMonitored())
                        .changePercent(overview.getExamsChangePercent())
                        .changeDirection(overview.getExamsChangeDirection())
                        .changeLabel(overview.getExamsChangeLabel())
                        .build())
                .totalFlaggedEvents(AnalyticsMetricCardDto.builder()
                        .value(overview.getTotalFlaggedEvents())
                        .changePercent(overview.getFlagsChangePercent())
                        .changeDirection(overview.getFlagsChangeDirection())
                        .changeLabel(overview.getFlagsChangeLabel())
                        .build())
                .avgIntegrityScore(AnalyticsIntegrityScoreDto.builder()
                        .value(overview.getAvgIntegrityScore())
                        .changePercent(overview.getIntegrityChangePercent())
                        .changeDirection(overview.getIntegrityChangeDirection())
                        .changeLabel(overview.getIntegrityChangeLabel())
                        .build())
                .mostCommonFlag(AnalyticsMostCommonFlagDto.builder()
                        .label(overview.getMostCommonFlagLabel())
                        .sharePercent(overview.getMostCommonFlagSharePercent())
                        .icon(overview.getMostCommonFlagIcon())
                        .build())
                .trends(AnalyticsTrendsDto.builder()
                        .title("Integrity Event Trends")
                        .subtitle(overview.getTrendSubtitle())
                        .granularity(overview.getTrendGranularity())
                        .points(mapTrendPoints(overview.getTrendPoints()))
                        .build())
                .topBehaviors(mapBehaviors(overview.getBehaviors()))
                .build();
    }

    private List<AnalyticsTrendPointDto> mapTrendPoints(List<LecturerAnalyticsTrendPoint> points) {
        return points.stream()
                .map(point -> AnalyticsTrendPointDto.builder()
                        .label(point.getLabel())
                        .monitoredSessions(point.getMonitoredSessions())
                        .flaggedEvents(point.getFlaggedEvents())
                        .alert(point.isAlert())
                        .build())
                .toList();
    }

    private List<AnalyticsBehaviorDto> mapBehaviors(List<LecturerAnalyticsBehavior> behaviors) {
        return behaviors.stream()
                .map(behavior -> AnalyticsBehaviorDto.builder()
                        .behaviorCode(behavior.getBehaviorCode())
                        .label(behavior.getLabel())
                        .eventCount(behavior.getEventCount())
                        .icon(behavior.getIcon())
                        .tone(behavior.getTone())
                        .build())
                .toList();
    }
}
