package com.backend.observerr.lecturer.dashboard;

import com.backend.observerr.auth.model.User;
import com.backend.observerr.auth.model.UserRepository;
import com.backend.observerr.exam.model.Exam;
import com.backend.observerr.exam.repository.ExamEnrollmentRepository;
import com.backend.observerr.exam.repository.ExamRepository;
import com.backend.observerr.integrity.model.ExamSession;
import com.backend.observerr.integrity.model.ExamSessionStatus;
import com.backend.observerr.integrity.model.IntegrityEvent;
import com.backend.observerr.integrity.repository.ExamSessionRepository;
import com.backend.observerr.lecturer.dashboard.dto.LiveExamSessionsResponse;
import com.backend.observerr.lecturer.dashboard.dto.LiveSessionStatsDto;
import com.backend.observerr.lecturer.dashboard.dto.MonitoredStudentDto;
import com.backend.observerr.lecturer.students.model.RiskLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LecturerLiveMonitoringService {

    private final ExamRepository examRepository;
    private final ExamEnrollmentRepository examEnrollmentRepository;
    private final ExamSessionRepository examSessionRepository;
    private final UserRepository userRepository;
    private final LecturerSessionInsights sessionInsights;

    @Transactional(readOnly = true)
    public LiveExamSessionsResponse getLiveSessions(User lecturer, Long examId) {
        Exam exam = examRepository.findByIdAndLecturerId(examId, lecturer.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam not found"));

        List<Long> enrolledStudentIds = examEnrollmentRepository.findStudentIdsByExamId(examId);
        List<ExamSession> sessions = examSessionRepository.findByExamIdAndLecturerId(examId, lecturer.getId());

        Map<Long, ExamSession> sessionByStudent = sessions.stream()
                .collect(Collectors.toMap(ExamSession::getStudentId, session -> session, (a, b) ->
                        a.getStartedAt().isAfter(b.getStartedAt()) ? a : b));

        List<Long> studentIds = enrolledStudentIds.isEmpty()
                ? sessions.stream().map(ExamSession::getStudentId).distinct().toList()
                : enrolledStudentIds;
        Map<Long, User> usersById = userRepository.findAllById(studentIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        List<MonitoredStudentDto> students = new ArrayList<>();
        int highRisk = 0;
        int warnings = 0;
        int active = 0;
        int scoreSum = 0;

        for (Long studentId : studentIds) {
            User student = usersById.get(studentId);
            if (student == null) {
                continue;
            }
            ExamSession session = sessionByStudent.get(studentId);
            MonitoredStudentDto row = toMonitoredStudent(student, session);
            students.add(row);

            if (session != null && session.getStatus() == ExamSessionStatus.IN_PROGRESS) {
                active++;
            }
            scoreSum += row.getIntegrityScore();
            if ("HIGH".equals(row.getRiskLevel())) {
                highRisk++;
            } else if ("MEDIUM".equals(row.getRiskLevel())) {
                warnings++;
            }
        }

        students.sort(Comparator.comparingInt(MonitoredStudentDto::getIntegrityScore));

        int total = Math.max(studentIds.size(), exam.getEnrolledCount());
        int avgScore = students.isEmpty() ? 94 : scoreSum / students.size();

        LiveSessionStatsDto stats = LiveSessionStatsDto.builder()
                .active(active)
                .total(total)
                .highRisk(highRisk)
                .warnings(warnings)
                .networkStability(Math.min(99, Math.max(70, avgScore)))
                .build();

        return LiveExamSessionsResponse.builder()
                .examId(examId)
                .stats(stats)
                .students(students)
                .build();
    }

    private MonitoredStudentDto toMonitoredStudent(User student, ExamSession session) {
        int score = session == null ? 100 : sessionInsights.resolveIntegrityScore(session);
        boolean forcedHighRisk = session != null && session.isRequiresReview();
        String riskLevel = forcedHighRisk
                ? RiskLevel.HIGH.name()
                : RiskLevel.fromIntegrityScore(score).name();
        IntegrityEvent latestEvent = session == null
                ? null
                : sessionInsights.latestEvent(session.getId());

        LiveStatus liveStatus = mapLiveStatus(session, latestEvent);

        return MonitoredStudentDto.builder()
                .studentId(student.getId())
                .studentNumber(formatStudentNumber(student.getInstitutionalId()))
                .name(fullName(student.getFirstName(), student.getLastName()))
                .initials(initials(student.getFirstName(), student.getLastName()))
                .liveStatus(liveStatus.code())
                .liveStatusLabel(liveStatus.label())
                .riskLevel(riskLevel)
                .lastEvent(formatLastEvent(latestEvent))
                .latestSessionId(session != null ? session.getId().toString() : null)
                .integrityScore(score)
                .build();
    }

    private LiveStatus mapLiveStatus(ExamSession session, IntegrityEvent latestEvent) {
        if (session == null) {
            return new LiveStatus("NOT_STARTED", "Not joined");
        }
        if (session.getStatus() == ExamSessionStatus.COMPLETED) {
            return new LiveStatus("COMPLETED", "Completed");
        }
        if (latestEvent == null) {
            return new LiveStatus("ACTIVE", "Active - Monitoring");
        }
        String code = latestEvent.getEventCode();
        if (code != null && code.contains("TAB")) {
            return new LiveStatus("TAB_OUT", "Active - Tab Out");
        }
        if (code != null && (code.contains("FACE") || code.contains("CAMERA"))) {
            return new LiveStatus("FACE_ISSUE", "Active - Face Issue");
        }
        return new LiveStatus("ACTIVE", "Active - Monitoring");
    }

    private String formatLastEvent(IntegrityEvent event) {
        if (event == null) {
            return "No events yet";
        }
        String ago = formatTimeAgo(event.getOccurredAt());
        return event.getTitle() + " (" + ago + ")";
    }

    private String formatTimeAgo(Instant occurredAt) {
        long minutes = ChronoUnit.MINUTES.between(occurredAt, Instant.now());
        if (minutes < 1) {
            return "just now";
        }
        if (minutes < 60) {
            return minutes + "m ago";
        }
        return (minutes / 60) + "h ago";
    }

    private String formatStudentNumber(String institutionalId) {
        if (institutionalId == null) {
            return "";
        }
        return institutionalId.startsWith("STU-") ? institutionalId : "STU-" + institutionalId;
    }

    private String fullName(String firstName, String lastName) {
        return ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
    }

    private String initials(String firstName, String lastName) {
        String first = firstName == null || firstName.isBlank() ? "?" : firstName.substring(0, 1);
        String last = lastName == null || lastName.isBlank() ? "?" : lastName.substring(0, 1);
        return (first + last).toUpperCase(Locale.ROOT);
    }

    private record LiveStatus(String code, String label) {}
}
