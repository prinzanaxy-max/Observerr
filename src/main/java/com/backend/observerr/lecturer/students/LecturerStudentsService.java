package com.backend.observerr.lecturer.students;

import com.backend.observerr.auth.model.User;
import com.backend.observerr.auth.model.UserRepository;
import com.backend.observerr.exam.model.Exam;
import com.backend.observerr.exam.repository.ExamRepository;
import com.backend.observerr.integrity.IntegritySessionService;
import com.backend.observerr.integrity.model.ExamSession;
import com.backend.observerr.integrity.model.IntegrityEvent;
import com.backend.observerr.lecturer.students.dto.*;
import com.backend.observerr.lecturer.students.model.LecturerCourse;
import com.backend.observerr.lecturer.students.model.ProctoringSession;
import com.backend.observerr.lecturer.students.model.ProctoringSessionEvent;
import com.backend.observerr.lecturer.students.model.RiskLevel;
import com.backend.observerr.lecturer.students.repository.LecturerCourseRepository;
import com.backend.observerr.lecturer.students.repository.LecturerStudentRow;
import com.backend.observerr.lecturer.students.repository.ProctoringSessionEventRepository;
import com.backend.observerr.lecturer.students.repository.ProctoringSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;

@Service
@RequiredArgsConstructor
public class LecturerStudentsService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("hh:mm a", Locale.US);

    private final LecturerCourseRepository lecturerCourseRepository;
    private final ProctoringSessionRepository proctoringSessionRepository;
    private final ProctoringSessionEventRepository proctoringSessionEventRepository;
    private final UserRepository userRepository;
    private final IntegritySessionService integritySessionService;
    private final ExamRepository examRepository;

    @Transactional(readOnly = true)
    public LecturerStudentsPageDto getStudents(User lecturer, int page, int size, String search, String course) {
        String courseFilter = normalizeCourseFilter(course);
        String searchFilter = normalizeSearch(search);

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        var roster = lecturerCourseRepository
                .findRealRoster(lecturer.getId(), courseFilter, searchFilter,
                        PageRequest.of(safePage, safeSize));
        List<LecturerStudentDto> pageContent = roster.getContent()
                .stream()
                .map(this::toStudentDto)
                .toList();

        long totalElements = roster.getTotalElements();
        int totalPages = roster.getTotalPages();
        int fromIndex = safePage * safeSize;
        int toIndex = fromIndex + pageContent.size();
        int from = pageContent.isEmpty() ? 0 : fromIndex + 1;
        int to = pageContent.isEmpty() ? 0 : toIndex;

        List<String> availableCourses = examRepository.findCourseLabelsByLecturerId(lecturer.getId());

        return LecturerStudentsPageDto.builder()
                .content(pageContent)
                .page(safePage)
                .size(safeSize)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .from(from)
                .to(to)
                .availableCourses(availableCourses)
                .build();
    }

    @Transactional(readOnly = true)
    public ProctoringSessionDetailDto getSessionDetail(User lecturer, String sessionId) {
        if (isUuid(sessionId)) {
            return getExamSessionDetail(lecturer, UUID.fromString(sessionId));
        }
        try {
            return getLegacyProctoringSessionDetail(lecturer, Long.parseLong(sessionId));
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found");
        }
    }

    private ProctoringSessionDetailDto getExamSessionDetail(User lecturer, UUID sessionId) {
        ExamSession session = integritySessionService.getSessionForLecturer(lecturer, sessionId);
        Exam exam = examRepository.findById(session.getExamId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam not found"));
        User student = userRepository.findById(session.getStudentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        List<ProctoringSessionEventDto> events = integritySessionService.getSessionEvents(sessionId)
                .stream()
                .map(this::toIntegrityEventDto)
                .toList();

        int integrityScore = session.getFinalScore() != null
                ? session.getFinalScore()
                : events.isEmpty() ? session.getStartingScore() : events.get(events.size() - 1).getScoreAfter();

        return ProctoringSessionDetailDto.builder()
                .sessionId(session.getId().toString())
                .studentId(student.getId())
                .studentNumber(formatStudentNumber(student.getInstitutionalId()))
                .studentName(fullName(student.getFirstName(), student.getLastName()))
                .initials(initials(student.getFirstName(), student.getLastName()))
                .assessmentTitle(exam.getTitle())
                .courseCode(exam.getCourseCode())
                .courseName(exam.getCourseName())
                .courseLabel(formatCourseLabel(exam.getCourseCode(), exam.getCourseName()))
                .integrityScore(integrityScore)
                .requiresReview(session.isRequiresReview())
                .duration(formatDuration(integritySessionService.computeDurationMinutes(session)))
                .totalFlags(session.getTotalEvents())
                .deviceFlags(countEventsWithCodes(events, "DEVICE", "TAB_BLUR_NO_FACE", "CAMERA_FEED_FROZEN", "CAMERA_PERMISSION_LOST"))
                .absenceFlags(countEventsWithCodes(events, "FACE_ABSENT", "TAB_BLUR_NO_FACE"))
                .sessionDate(DATE_FORMAT.format(session.getStartedAt().atZone(ZoneId.systemDefault()).toLocalDate()))
                .events(events)
                .build();
    }

    private ProctoringSessionDetailDto getLegacyProctoringSessionDetail(User lecturer, Long sessionId) {
        ProctoringSession session = proctoringSessionRepository.findByIdAndLecturerId(sessionId, lecturer.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        User student = userRepository.findById(session.getStudentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        List<ProctoringSessionEventDto> events = proctoringSessionEventRepository
                .findBySessionIdOrderBySortOrderAsc(sessionId)
                .stream()
                .map(this::toEventDto)
                .toList();

        return ProctoringSessionDetailDto.builder()
                .sessionId(String.valueOf(session.getId()))
                .studentId(student.getId())
                .studentNumber(formatStudentNumber(student.getInstitutionalId()))
                .studentName(fullName(student.getFirstName(), student.getLastName()))
                .initials(initials(student.getFirstName(), student.getLastName()))
                .assessmentTitle(session.getAssessmentTitle())
                .courseCode(session.getCourseCode())
                .courseName(session.getCourseName())
                .courseLabel(session.getCourseCode() + ": " + session.getCourseName())
                .integrityScore(session.getIntegrityScore())
                .requiresReview(session.getIntegrityScore() < 60)
                .duration(formatDuration(session.getDurationMinutes()))
                .totalFlags(session.getTotalFlags())
                .deviceFlags(session.getDeviceFlags())
                .absenceFlags(session.getAbsenceFlags())
                .sessionDate(DATE_FORMAT.format(session.getSessionDate()))
                .events(events)
                .build();
    }

    private LecturerStudentDto toStudentDto(LecturerStudentRow row) {
        int avgIntegrity = (int) Math.round(row.getAvgIntegrity());
        String firstName = defaultName(row.getFirstName());
        String lastName = defaultName(row.getLastName());
        String fullName = fullName(firstName, lastName);

        return LecturerStudentDto.builder()
                .id(row.getStudentId())
                .studentNumber(formatStudentNumber(row.getInstitutionalId()))
                .firstName(firstName)
                .lastName(lastName)
                .fullName(fullName)
                .initials(initials(firstName, lastName))
                .courseCode(row.getCourseCode())
                .courseName(row.getCourseName())
                .courseLabel(row.getCourseCode() + ": " + row.getCourseName())
                .examsTaken(row.getExamsTaken())
                .avgIntegrityScore(avgIntegrity)
                .riskLevel(RiskLevel.fromIntegrityScore(avgIntegrity).name())
                .lastActive(formatLastActive(row.getLastActiveDate()))
                .latestSessionId(row.getLatestSessionId())
                .build();
    }

    private ProctoringSessionEventDto toEventDto(ProctoringSessionEvent event) {
        return ProctoringSessionEventDto.builder()
                .id(event.getId())
                .eventCode(event.getEventType())
                .time(TIME_FORMAT.format(event.getEventTime()))
                .timestamp(null)
                .eventType(event.getEventType())
                .severity(mapLegacySeverity(event.getSeverity()))
                .title(event.getTitle())
                .description(event.getDescription())
                .pointsDeducted(event.getPointsDeducted() != null ? event.getPointsDeducted().intValue() : null)
                .scoreAfter(null)
                .durationMs(null)
                .hasSnapshot(event.isHasSnapshot())
                .build();
    }

    private ProctoringSessionEventDto toIntegrityEventDto(IntegrityEvent event) {
        return ProctoringSessionEventDto.builder()
                .id(event.getId())
                .eventCode(event.getEventCode())
                .time(TIME_FORMAT.format(event.getOccurredAt().atZone(ZoneId.systemDefault()).toLocalTime()))
                .timestamp(event.getOccurredAt().toString())
                .eventType(event.getEventCode())
                .severity(normalizeUiSeverity(event.getSeverity()))
                .title(event.getTitle())
                .description(event.getDescription())
                .pointsDeducted(event.getPointsDeducted())
                .scoreAfter(event.getScoreAfter())
                .durationMs(event.getDurationMs())
                .hasSnapshot(false)
                .build();
    }

    private String mapLegacySeverity(String severity) {
        return normalizeUiSeverity(severity);
    }

    /** Map stored severities to the lecturer UI contract. */
    private String normalizeUiSeverity(String severity) {
        if (severity == null || severity.isBlank()) {
            return "NEUTRAL";
        }
        return switch (severity.trim().toUpperCase()) {
            case "HIGH", "CRITICAL", "ERROR", "DANGER" -> "DANGER";
            case "MEDIUM", "WARN", "WARNING" -> "WARNING";
            case "SUCCESS", "OK" -> "SUCCESS";
            case "LOW", "INFO", "NEUTRAL" -> "NEUTRAL";
            default -> severity.trim().toUpperCase();
        };
    }

    private boolean isUuid(String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private int countEventsWithCodes(List<ProctoringSessionEventDto> events, String... codes) {
        return (int) events.stream()
                .filter(event -> {
                    String code = event.getEventCode();
                    if (code == null) {
                        return false;
                    }
                    for (String candidate : codes) {
                        if (code.contains(candidate)) {
                            return true;
                        }
                    }
                    return false;
                })
                .count();
    }

    private String formatCourseLabel(String courseCode, String courseName) {
        if (courseCode == null || courseCode.isBlank()) {
            return courseName;
        }
        return courseCode + ": " + courseName;
    }

    private String normalizeCourseFilter(String course) {
        if (course == null || course.isBlank() || "ALL".equalsIgnoreCase(course)) {
            return null;
        }
        return course.trim();
    }

    private String normalizeSearch(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        return search.trim();
    }

    private String formatStudentNumber(String institutionalId) {
        if (institutionalId == null) {
            return "";
        }
        return institutionalId.startsWith("STU-")
                ? institutionalId.substring(4)
                : institutionalId;
    }

    private String fullName(String firstName, String lastName) {
        return (firstName + " " + lastName).trim();
    }

    private String initials(String firstName, String lastName) {
        String first = firstName == null || firstName.isBlank() ? "?" : firstName.substring(0, 1);
        String last = lastName == null || lastName.isBlank() ? "?" : lastName.substring(0, 1);
        return (first + last).toUpperCase(Locale.ROOT);
    }

    private String defaultName(String value) {
        return value == null ? "" : value;
    }

    private String formatDuration(int durationMinutes) {
        int hours = durationMinutes / 60;
        int minutes = durationMinutes % 60;
        if (hours > 0 && minutes > 0) {
            return hours + "h " + String.format(Locale.US, "%02dm", minutes);
        }
        if (hours > 0) {
            return hours + "h 00m";
        }
        return minutes + "m";
    }

    private String formatLastActive(LocalDate lastActiveDate) {
        if (lastActiveDate == null) {
            return "Unknown";
        }
        long days = ChronoUnit.DAYS.between(lastActiveDate, LocalDate.now());
        if (days <= 0) {
            return "Today";
        }
        if (days == 1) {
            return "1 day ago";
        }
        return days + " days ago";
    }
}
