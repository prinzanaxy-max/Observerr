package com.backend.observerr.lecturer.students;

import com.backend.observerr.auth.model.User;
import com.backend.observerr.auth.model.UserRepository;
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
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class LecturerStudentsService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("hh:mm a", Locale.US);

    private final LecturerCourseRepository lecturerCourseRepository;
    private final ProctoringSessionRepository proctoringSessionRepository;
    private final ProctoringSessionEventRepository proctoringSessionEventRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public LecturerStudentsPageDto getStudents(User lecturer, int page, int size, String search, String course) {
        String courseFilter = normalizeCourseFilter(course);
        String searchFilter = normalizeSearch(search);

        List<LecturerStudentDto> allStudents = lecturerCourseRepository
                .findRoster(lecturer.getId(), courseFilter, searchFilter)
                .stream()
                .map(this::toStudentDto)
                .toList();

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        int totalElements = allStudents.size();
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / safeSize);
        int fromIndex = Math.min(safePage * safeSize, totalElements);
        int toIndex = Math.min(fromIndex + safeSize, totalElements);

        List<LecturerStudentDto> pageContent = allStudents.subList(fromIndex, toIndex);
        int from = pageContent.isEmpty() ? 0 : fromIndex + 1;
        int to = pageContent.isEmpty() ? 0 : toIndex;

        List<String> availableCourses = lecturerCourseRepository.findByLecturerIdOrderByCourseCodeAsc(lecturer.getId())
                .stream()
                .map(courseEntry -> courseEntry.getCourseCode() + ":" + courseEntry.getCourseName())
                .toList();

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
    public ProctoringSessionDetailDto getSessionDetail(User lecturer, Long sessionId) {
        ProctoringSession session = proctoringSessionRepository.findByIdAndLecturerId(sessionId, lecturer.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        User student = userRepository.findById(session.getStudentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        List<ProctoringSessionEventDto> events = proctoringSessionEventRepository
                .findBySessionIdOrderBySortOrderAsc(sessionId)
                .stream()
                .map(this::toEventDto)
                .toList();

        String fullName = fullName(student.getFirstName(), student.getLastName());

        return ProctoringSessionDetailDto.builder()
                .sessionId(session.getId())
                .studentId(student.getId())
                .studentNumber(formatStudentNumber(student.getInstitutionalId()))
                .studentName(fullName)
                .initials(initials(student.getFirstName(), student.getLastName()))
                .assessmentTitle(session.getAssessmentTitle())
                .courseCode(session.getCourseCode())
                .courseName(session.getCourseName())
                .courseLabel(session.getCourseCode() + ": " + session.getCourseName())
                .integrityScore(session.getIntegrityScore())
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
                .time(TIME_FORMAT.format(event.getEventTime()))
                .eventType(event.getEventType())
                .severity(event.getSeverity())
                .title(event.getTitle())
                .description(event.getDescription())
                .pointsDeducted(event.getPointsDeducted() != null ? event.getPointsDeducted().intValue() : null)
                .hasSnapshot(event.isHasSnapshot())
                .build();
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
