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
import java.util.Optional;
import java.util.Set;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.data.domain.PageRequest;
import com.backend.observerr.integrity.repository.IntegrityEventRepository;
import com.backend.observerr.lecturer.analytics.dto.IntegrityReportPageDto;
import com.backend.observerr.lecturer.analytics.dto.IntegrityReportEventDto;
import org.springframework.jdbc.core.JdbcTemplate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LecturerAnalyticsService {

    private static final Set<String> SUPPORTED_PERIODS = Set.of("7D", "30D", "3M");

    private final LecturerAnalyticsOverviewRepository overviewRepository;
    private final IntegrityEventRepository integrityEventRepository;
    private final JdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = "lecturerAnalyticsOverview",
            key = "#lecturer.id + ':' + #period"
    )
    public LecturerAnalyticsOverviewResponse getOverview(User lecturer, String period) {
        String normalizedPeriod = normalizePeriod(period);
        LecturerAnalyticsOverviewResponse live = deriveOverview(lecturer.getId(), normalizedPeriod);
        if (live != null) {
            return live;
        }
        LecturerAnalyticsOverview overview = overviewRepository
                .findByLecturerIdAndPeriod(lecturer.getId(), normalizedPeriod)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Analytics overview not found for period " + normalizedPeriod));

        overview.getTrendPoints().size();
        overview.getBehaviors().size();

        return toResponse(overview);
    }

    private LecturerAnalyticsOverviewResponse deriveOverview(Long lecturerId, String period) {
        int days = (int) periodDays(period);
        Instant end = Instant.now();
        Instant start = end.minus(days, ChronoUnit.DAYS);
        Instant previousStart = start.minus(days, ChronoUnit.DAYS);
        Map<String, Object> current = aggregate(lecturerId, start, end);
        long sessions = ((Number) current.get("sessions")).longValue();
        if (sessions == 0) {
            return null;
        }
        Map<String, Object> previous = aggregate(lecturerId, previousStart, start);
        long events = ((Number) current.get("events")).longValue();
        BigDecimal average = decimal(current.get("average"));
        BigDecimal previousAverage = decimal(previous.get("average"));
        List<Map<String, Object>> behaviorRows = jdbcTemplate.queryForList("""
                SELECT ie.event_code AS code, COUNT(*) AS count
                FROM integrity_events ie
                JOIN exam_sessions es ON es.id = ie.session_id
                JOIN exams e ON e.id = es.exam_id
                WHERE e.lecturer_id = ? AND ie.occurred_at >= ? AND ie.occurred_at < ?
                GROUP BY ie.event_code ORDER BY COUNT(*) DESC
                """, lecturerId, Timestamp.from(start), Timestamp.from(end));
        List<AnalyticsBehaviorDto> behaviors = behaviorRows.stream().limit(5)
                .map(row -> behavior((String) row.get("code"), ((Number) row.get("count")).intValue()))
                .toList();
        AnalyticsBehaviorDto common = behaviors.isEmpty() ? null : behaviors.get(0);
        int commonShare = common == null || events == 0 ? 0
                : (int) Math.round(common.getEventCount() * 100.0 / events);

        return LecturerAnalyticsOverviewResponse.builder()
                .period(period)
                .totalExamsMonitored(metric(sessions,
                        ((Number) previous.get("sessions")).longValue(), "from previous period"))
                .totalFlaggedEvents(metric(events,
                        ((Number) previous.get("events")).longValue(), "from previous period"))
                .avgIntegrityScore(AnalyticsIntegrityScoreDto.builder()
                        .value(average).changePercent(percentChange(average, previousAverage))
                        .changeDirection(direction(average.compareTo(previousAverage)))
                        .changeLabel("vs previous period").build())
                .mostCommonFlag(AnalyticsMostCommonFlagDto.builder()
                        .label(common == null ? "No flagged events" : common.getLabel())
                        .sharePercent(commonShare)
                        .icon(common == null ? "verified" : common.getIcon()).build())
                .trends(AnalyticsTrendsDto.builder()
                        .title("Integrity Event Trends")
                        .subtitle("Actual flagged events vs monitored sessions")
                        .granularity(days > 30 ? "WEEK" : "DAY")
                        .points(trendPoints(lecturerId, start, end, days > 30)).build())
                .topBehaviors(behaviors).build();
    }

    private Map<String, Object> aggregate(Long lecturerId, Instant start, Instant end) {
        return jdbcTemplate.queryForMap("""
                SELECT COUNT(DISTINCT es.id) AS sessions,
                       COUNT(ie.id) AS events,
                       COALESCE(AVG(COALESCE(es.final_score,
                           GREATEST(0, es.starting_score - es.total_deductions))), 100) AS average
                FROM exam_sessions es
                JOIN exams e ON e.id = es.exam_id
                LEFT JOIN integrity_events ie ON ie.session_id = es.id
                    AND ie.occurred_at >= ? AND ie.occurred_at < ?
                WHERE e.lecturer_id = ? AND es.started_at >= ? AND es.started_at < ?
                """, Timestamp.from(start), Timestamp.from(end), lecturerId,
                Timestamp.from(start), Timestamp.from(end));
    }

    private List<AnalyticsTrendPointDto> trendPoints(
            Long lecturerId, Instant start, Instant end, boolean weekly) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT CAST(es.started_at AS DATE) AS day,
                       COUNT(DISTINCT es.id) AS sessions, COUNT(ie.id) AS events
                FROM exam_sessions es
                JOIN exams e ON e.id = es.exam_id
                LEFT JOIN integrity_events ie ON ie.session_id = es.id
                WHERE e.lecturer_id = ? AND es.started_at >= ? AND es.started_at < ?
                GROUP BY CAST(es.started_at AS DATE) ORDER BY day
                """, lecturerId, Timestamp.from(start), Timestamp.from(end));
        Map<String, int[]> grouped = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            LocalDate day = ((java.sql.Date) row.get("day")).toLocalDate();
            String label = weekly
                    ? "W" + Math.max(1, (int) (java.time.temporal.ChronoUnit.DAYS.between(
                            start.atZone(ZoneOffset.UTC).toLocalDate(), day) / 7 + 1))
                    : day.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.US);
            int[] values = grouped.computeIfAbsent(label, ignored -> new int[2]);
            values[0] += ((Number) row.get("sessions")).intValue();
            values[1] += ((Number) row.get("events")).intValue();
        }
        List<AnalyticsTrendPointDto> points = new ArrayList<>();
        grouped.forEach((label, values) -> points.add(AnalyticsTrendPointDto.builder()
                .label(label).monitoredSessions(values[0]).flaggedEvents(values[1])
                .alert(values[1] > values[0]).build()));
        return points;
    }

    private AnalyticsMetricCardDto metric(long value, long previous, String label) {
        BigDecimal change = previous == 0 ? null : BigDecimal.valueOf(value - previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(previous), 2, RoundingMode.HALF_UP);
        return AnalyticsMetricCardDto.builder().value(value).changePercent(change)
                .changeDirection(direction(Long.compare(value, previous))).changeLabel(label).build();
    }

    private AnalyticsBehaviorDto behavior(String code, int count) {
        String label = java.util.Arrays.stream(code.toLowerCase(Locale.ROOT).split("_"))
                .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
                .collect(java.util.stream.Collectors.joining(" "));
        String icon = code.contains("FACE") ? "visibility_off"
                : code.contains("AUDIO") ? "mic_off"
                : code.contains("DEVICE") ? "smartphone" : "warning";
        String tone = code.contains("CRITICAL") || code.contains("DEVICE") ? "error"
                : code.contains("AUDIO") ? "warning" : "neutral";
        return AnalyticsBehaviorDto.builder().behaviorCode(code).label(label)
                .eventCount(count).icon(icon).tone(tone).build();
    }

    private BigDecimal decimal(Object value) {
        return value == null ? BigDecimal.ZERO : new BigDecimal(value.toString())
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal percentChange(BigDecimal current, BigDecimal previous) {
        if (previous.signum() == 0) return null;
        return current.subtract(previous).multiply(BigDecimal.valueOf(100))
                .divide(previous, 2, RoundingMode.HALF_UP);
    }

    private String direction(int comparison) {
        return comparison > 0 ? "UP" : comparison < 0 ? "DOWN" : "STABLE";
    }

    @Transactional(readOnly = true)
    public Optional<LecturerAnalyticsOverviewResponse> findOverview(User lecturer, String period) {
        String normalizedPeriod = normalizePeriod(period);
        return overviewRepository.findByLecturerIdAndPeriod(lecturer.getId(), normalizedPeriod)
                .map(overview -> {
                    overview.getTrendPoints().size();
                    overview.getBehaviors().size();
                    return toResponse(overview);
                });
    }

    @Transactional(readOnly = true)
    public IntegrityReportPageDto getIntegrityEvents(
            User lecturer, String period, int page, int size,
            String search, String eventType, String severity) {
        String normalizedPeriod = normalizePeriod(period);
        Instant start = Instant.now().minus(periodDays(normalizedPeriod), ChronoUnit.DAYS);
        int safePage = Math.max(0, page);
        int safeSize = Math.min(100, Math.max(1, size));
        var result = integrityEventRepository.findReport(
                lecturer.getId(), start, blankToNull(search), blankToNull(eventType),
                blankToNull(severity), PageRequest.of(safePage, safeSize));
        return IntegrityReportPageDto.builder()
                .content(result.getContent().stream().map(row -> IntegrityReportEventDto.builder()
                        .id(row.getId()).sessionId(row.getSessionId().toString())
                        .studentId(row.getStudentId())
                        .studentName(row.getStudentName() == null || row.getStudentName().isBlank()
                                ? "Student " + row.getStudentId() : row.getStudentName())
                        .examId(row.getExamId()).examTitle(row.getExamTitle())
                        .eventType(row.getEventType()).severity(normalizeSeverity(row.getSeverity()))
                        .occurredAt(row.getOccurredAt().toString())
                        .pointsDeducted(row.getPointsDeducted()).build()).toList())
                .page(result.getNumber()).size(result.getSize())
                .totalElements(result.getTotalElements()).totalPages(result.getTotalPages())
                .eventTypes(integrityEventRepository.findReportEventTypes(lecturer.getId(), start))
                .build();
    }

    private long periodDays(String period) {
        return switch (period) {
            case "30D" -> 30;
            case "3M" -> 90;
            default -> 7;
        };
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeSeverity(String severity) {
        if (severity == null) return "NEUTRAL";
        return switch (severity.toUpperCase(Locale.ROOT)) {
            case "HIGH", "CRITICAL", "ERROR" -> "DANGER";
            case "MEDIUM", "WARN" -> "WARNING";
            case "LOW", "INFO" -> "NEUTRAL";
            default -> severity.toUpperCase(Locale.ROOT);
        };
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
