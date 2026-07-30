package com.backend.observerr.lecturer.dashboard;

import com.backend.observerr.auth.model.User;
import com.backend.observerr.auth.model.UserRepository;
import com.backend.observerr.exam.dto.LecturerExamDto;
import com.backend.observerr.exam.model.Exam;
import com.backend.observerr.exam.repository.ExamRepository;
import com.backend.observerr.exam.service.LecturerExamService;
import com.backend.observerr.integrity.model.ExamSession;
import com.backend.observerr.integrity.model.ExamSessionStatus;
import com.backend.observerr.integrity.repository.ExamSessionRepository;
import com.backend.observerr.lecturer.analytics.LecturerAnalyticsService;
import com.backend.observerr.lecturer.analytics.dto.AnalyticsBehaviorDto;
import com.backend.observerr.lecturer.analytics.dto.AnalyticsTrendPointDto;
import com.backend.observerr.lecturer.analytics.dto.LecturerAnalyticsOverviewResponse;
import com.backend.observerr.lecturer.dashboard.dto.*;
import com.backend.observerr.lecturer.students.model.ProctoringSession;
import com.backend.observerr.lecturer.students.model.RiskLevel;
import com.backend.observerr.lecturer.students.repository.ProctoringSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class LecturerDashboardService {

    private static final int REVIEW_THRESHOLD = 60;
    private static final int DASHBOARD_EXAM_CAP = 5;
    private static final int NEEDS_REVIEW_LIMIT = 5;

    private final LecturerExamService lecturerExamService;
    private final LecturerAnalyticsService lecturerAnalyticsService;
    private final ExamSessionRepository examSessionRepository;
    private final ProctoringSessionRepository proctoringSessionRepository;
    private final ExamRepository examRepository;
    private final UserRepository userRepository;
    private final LecturerSessionInsights sessionInsights;

    @Transactional(readOnly = true)
    public LecturerDashboardResponse getDashboard(User lecturer) {
        List<LecturerExamDto> allExams = lecturerExamService.listExams(lecturer, "ALL", null).getExams();

        LecturerExamDto liveExamDto = allExams.stream()
                .filter(exam -> "LIVE".equals(exam.getStatus()))
                .findFirst()
                .orElse(null);

        LiveExamBannerDto liveExam = liveExamDto == null
                ? null
                : buildLiveExamBanner(liveExamDto, lecturer.getId());

        List<NeedsReviewItemDto> needsReview = buildNeedsReview(lecturer.getId());

        ExamTabsDto examTabs = ExamTabsDto.builder()
                .live(capExams(allExams, "LIVE"))
                .upcoming(capExams(allExams, "UPCOMING"))
                .completed(capExams(allExams, "COMPLETED"))
                .build();

        Optional<LecturerAnalyticsOverviewResponse> analytics =
                lecturerAnalyticsService.findOverview(lecturer, "7D");

        IntegrityTrendSummaryDto integrityTrend = analytics
                .map(this::toIntegrityTrend)
                .orElse(IntegrityTrendSummaryDto.builder()
                        .changeLabel("No trend data")
                        .changeDirection("STABLE")
                        .points(List.of())
                        .build());

        List<TopFlaggedBehaviorDto> topFlaggedBehaviors = analytics
                .map(this::toTopBehaviors)
                .orElse(List.of());

        return LecturerDashboardResponse.builder()
                .liveExam(liveExam)
                .needsReview(needsReview)
                .examTabs(examTabs)
                .integrityTrend(integrityTrend)
                .topFlaggedBehaviors(topFlaggedBehaviors)
                .build();
    }

    @Transactional(readOnly = true)
    public List<NeedsReviewItemDto> getNeedsReview(User lecturer, int limit, Long examId) {
        List<NeedsReviewItemDto> items = buildNeedsReview(lecturer.getId());
        if (examId != null) {
            items = items.stream()
                    .filter(item -> examId.equals(item.getExamId()))
                    .toList();
        }
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        return items.stream().limit(safeLimit).toList();
    }

    private LiveExamBannerDto buildLiveExamBanner(LecturerExamDto examDto, Long lecturerId) {
        Exam exam = examRepository.findByIdAndLecturerId(examDto.getId(), lecturerId).orElseThrow();
        List<ExamSession> sessions = examSessionRepository.findByExamIdAndLecturerId(exam.getId(), lecturerId);

        List<ExamSession> activeSessions = sessions.stream()
                .filter(session -> session.getStatus() == ExamSessionStatus.IN_PROGRESS)
                .toList();

        int highRisk = 0;
        double scoreSum = 0;
        int scoreCount = 0;
        for (ExamSession session : activeSessions) {
            int score = resolveIntegrityScore(session);
            scoreSum += score;
            scoreCount++;
            if (score < REVIEW_THRESHOLD || session.isRequiresReview()) {
                highRisk++;
            }
        }

        double avgScore = scoreCount == 0 ? 0.0 : scoreSum / scoreCount;
        if (scoreCount == 0 && examDto.getEnrolledCount() > 0) {
            avgScore = 88.0;
        }

        long remainingSeconds = Math.max(0, Duration.between(Instant.now(), exam.getEndTime()).getSeconds());

        return LiveExamBannerDto.builder()
                .examId(exam.getId())
                .title(exam.getTitle())
                .courseCode(exam.getCourseCode())
                .status("LIVE")
                .remainingSeconds(remainingSeconds)
                .activeStudents(activeSessions.isEmpty() ? exam.getEnrolledCount() : activeSessions.size())
                .highRiskCount(highRisk)
                .avgIntegrityScore(Math.round(avgScore * 10.0) / 10.0)
                .liveMonitoringPath("/lecturer/exams/" + exam.getId() + "/live")
                .build();
    }

    private List<NeedsReviewItemDto> buildNeedsReview(Long lecturerId) {
        List<NeedsReviewItemDto> items = new ArrayList<>();

        for (ExamSession session : examSessionRepository.findNeedsReviewForLecturer(lecturerId, REVIEW_THRESHOLD)) {
            if (items.size() >= NEEDS_REVIEW_LIMIT) {
                break;
            }
            items.add(toNeedsReviewFromExamSession(session));
        }

        if (items.size() < NEEDS_REVIEW_LIMIT) {
            for (ProctoringSession legacy : proctoringSessionRepository.findNeedsReviewForLecturer(lecturerId, REVIEW_THRESHOLD)) {
                if (items.size() >= NEEDS_REVIEW_LIMIT) {
                    break;
                }
                items.add(toNeedsReviewFromLegacy(legacy));
            }
        }

        return items.stream()
                .sorted(Comparator.comparingInt(NeedsReviewItemDto::getIntegrityScore))
                .limit(NEEDS_REVIEW_LIMIT)
                .toList();
    }

    private NeedsReviewItemDto toNeedsReviewFromExamSession(ExamSession session) {
        User student = userRepository.findById(session.getStudentId()).orElseThrow();
        Exam exam = examRepository.findById(session.getExamId()).orElseThrow();
        int score = resolveIntegrityScore(session);

        return NeedsReviewItemDto.builder()
                .studentId(student.getId())
                .studentName(fullName(student.getFirstName(), student.getLastName()))
                .initials(initials(student.getFirstName(), student.getLastName()))
                .examTitle(exam.getTitle())
                .examId(exam.getId())
                .riskLevel(RiskLevel.fromIntegrityScore(score).name())
                .integrityScore(score)
                .latestSessionId(session.getId().toString())
                .requiresReview(session.isRequiresReview() || score < REVIEW_THRESHOLD)
                .build();
    }

    private NeedsReviewItemDto toNeedsReviewFromLegacy(ProctoringSession session) {
        User student = userRepository.findById(session.getStudentId()).orElseThrow();
        int score = session.getIntegrityScore();

        return NeedsReviewItemDto.builder()
                .studentId(student.getId())
                .studentName(fullName(student.getFirstName(), student.getLastName()))
                .initials(initials(student.getFirstName(), student.getLastName()))
                .examTitle(session.getAssessmentTitle())
                .examId(null)
                .riskLevel(RiskLevel.fromIntegrityScore(score).name())
                .integrityScore(score)
                .latestSessionId(String.valueOf(session.getId()))
                .requiresReview(true)
                .build();
    }

    private IntegrityTrendSummaryDto toIntegrityTrend(LecturerAnalyticsOverviewResponse overview) {
        var scoreCard = overview.getAvgIntegrityScore();
        String changeLabel = scoreCard.getChangeLabel();
        if (scoreCard.getChangePercent() != null && changeLabel != null && !changeLabel.isBlank()) {
            changeLabel = formatSignedPercent(scoreCard.getChangePercent()) + " " + changeLabel;
        }

        List<Integer> points = overview.getTrends().getPoints().stream()
                .map(AnalyticsTrendPointDto::getFlaggedEvents)
                .toList();

        return IntegrityTrendSummaryDto.builder()
                .changeLabel(changeLabel != null ? changeLabel : "Stable")
                .changeDirection(scoreCard.getChangeDirection())
                .points(points)
                .build();
    }

    private List<TopFlaggedBehaviorDto> toTopBehaviors(LecturerAnalyticsOverviewResponse overview) {
        List<AnalyticsBehaviorDto> behaviors = overview.getTopBehaviors();
        int totalEvents = behaviors.stream().mapToInt(AnalyticsBehaviorDto::getEventCount).sum();
        if (totalEvents == 0) {
            return List.of();
        }

        return behaviors.stream()
                .map(behavior -> TopFlaggedBehaviorDto.builder()
                        .behaviorCode(behavior.getBehaviorCode())
                        .label(behavior.getLabel())
                        .eventCount(behavior.getEventCount())
                        .sharePercent((int) Math.round(100.0 * behavior.getEventCount() / totalEvents))
                        .tone(behavior.getTone())
                        .icon(behavior.getIcon())
                        .build())
                .toList();
    }

    private List<LecturerExamDto> capExams(List<LecturerExamDto> exams, String status) {
        return exams.stream()
                .filter(exam -> status.equals(exam.getStatus()))
                .limit(DASHBOARD_EXAM_CAP)
                .toList();
    }

    int resolveIntegrityScore(ExamSession session) {
        return sessionInsights.resolveIntegrityScore(session);
    }

    private String formatSignedPercent(java.math.BigDecimal percent) {
        if (percent == null) {
            return "";
        }
        double value = percent.doubleValue();
        return (value >= 0 ? "+" : "") + value + "%";
    }

    private String fullName(String firstName, String lastName) {
        return ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
    }

    private String initials(String firstName, String lastName) {
        String first = firstName == null || firstName.isBlank() ? "?" : firstName.substring(0, 1);
        String last = lastName == null || lastName.isBlank() ? "?" : lastName.substring(0, 1);
        return (first + last).toUpperCase(Locale.ROOT);
    }
}
