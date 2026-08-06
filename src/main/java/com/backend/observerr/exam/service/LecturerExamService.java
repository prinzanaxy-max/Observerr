package com.backend.observerr.exam.service;

import com.backend.observerr.auth.model.User;
import com.backend.observerr.exam.ExamDisplayStatusResolver;
import com.backend.observerr.exam.dto.*;
import com.backend.observerr.exam.model.Exam;
import com.backend.observerr.exam.model.ExamDisplayStatus;
import com.backend.observerr.exam.model.ExamStatus;
import com.backend.observerr.exam.repository.ExamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class LecturerExamService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a", Locale.US);
    private static final DateTimeFormatter ISO_LOCAL = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final ExamRepository examRepository;
    private final ExamQuestionService examQuestionService;
    private final ExamEnrollmentService examEnrollmentService;

    @Transactional(readOnly = true)
    public LecturerExamListResponse listExams(User lecturer, String statusFilter, String search) {
        List<LecturerExamDto> exams = examRepository.findByLecturerIdOrderByStartTimeDesc(lecturer.getId())
                .stream()
                .map(this::toDto)
                .filter(dto -> matchesStatusFilter(dto.getStatus(), statusFilter))
                .filter(dto -> matchesSearch(dto, search))
                .sorted(examListComparator())
                .toList();

        return LecturerExamListResponse.builder()
                .exams(exams)
                .totalElements(exams.size())
                .build();
    }

    @Transactional(readOnly = true)
    public LecturerExamDto getExam(User lecturer, Long examId) {
        Exam exam = examRepository.findByIdAndLecturerId(examId, lecturer.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam not found"));
        return toDto(exam);
    }

    @Transactional
    public LecturerExamDto createExam(User lecturer, CreateExamRequest request) {
        ParsedCourse course = parseCourse(request.getCourse());
        Instant startTime = parseStartAt(request.getStartAt());
        int durationMinutes = request.getDurationMinutes();
        ExamSecurityDto security = request.getSecurity();
        if (request.isPublish()
                && (request.getStudentInstitutionalIds() == null
                    || request.getStudentInstitutionalIds().isEmpty())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "A published exam must enroll at least one student");
        }

        Exam exam = Exam.builder()
                .title(request.getTitle().trim())
                .lecturerId(lecturer.getId())
                .courseCode(course.code())
                .courseName(course.name())
                .durationMinutes(durationMinutes)
                .startTime(startTime)
                .endTime(startTime.plusSeconds(durationMinutes * 60L))
                .status(ExamStatus.SCHEDULED)
                .webcamMonitoring(security.isWebcamMonitoring())
                .tabSwitchTracking(security.isTabSwitchTracking())
                .blockCopyPaste(security.isBlockCopyPaste())
                .published(request.isPublish())
                .enrolledCount(0)
                .capacityCount(null)
                .activeFlagsCount(0)
                .startNotificationsSent(false)
                .build();

        Exam saved = examRepository.save(exam);
        examQuestionService.createQuestions(saved.getId(), request.getQuestions());
        examEnrollmentService.replaceEnrollments(
                saved.getId(), lecturer.getId(), request.getStudentInstitutionalIds());
        if (saved.isPublished() && !examQuestionService.loadQuestions(saved.getId()).isEmpty()) {
            return toDto(saved);
        }
        if (saved.isPublished()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "A published exam must contain at least one question");
        }
        return toDto(saved);
    }

    @Transactional
    public LecturerExamDto publishExam(User lecturer, Long examId) {
        Exam exam = examRepository.findByIdAndLecturerId(examId, lecturer.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam not found"));
        if (exam.isPublished()) {
            return toDto(exam);
        }
        if (examQuestionService.loadQuestions(examId).isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "A published exam must contain at least one question");
        }
        if (exam.getEnrolledCount() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "A published exam must enroll at least one student");
        }
        if (!Instant.now().isBefore(exam.getStartTime())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Past exams cannot be published");
        }
        exam.setPublished(true);
        return toDto(examRepository.save(exam));
    }

    private LecturerExamDto toDto(Exam exam) {
        ExamDisplayStatus displayStatus = computeDisplayStatus(exam);
        LocalDateTime startLocal = toLocalDateTime(exam.getStartTime());
        LocalDateTime endLocal = startLocal.plusMinutes(resolveDurationMinutes(exam));
        ExamSecurityDto security = toSecurityDto(exam);

        return LecturerExamDto.builder()
                .id(exam.getId())
                .title(exam.getTitle())
                .courseCode(exam.getCourseCode())
                .courseName(exam.getCourseName())
                .courseLabel(formatCourseLabel(exam.getCourseCode(), exam.getCourseName()))
                .term(formatTerm(startLocal))
                .schedule(formatSchedule(startLocal, endLocal))
                .status(displayStatus.name())
                .enrollment(formatEnrollment(exam, displayStatus))
                .enrolledCount(exam.getEnrolledCount())
                .capacityCount(exam.getCapacityCount())
                .activeFlagsCount(exam.getActiveFlagsCount())
                .startAt(startLocal.format(ISO_LOCAL))
                .durationMinutes(resolveDurationMinutes(exam))
                .published(exam.isPublished())
                .questionCount(examQuestionService.loadQuestions(exam.getId()).size())
                .security(security)
                .detail(buildDetail(exam, displayStatus, security))
                .build();
    }

    private ExamSecurityDto toSecurityDto(Exam exam) {
        return ExamSecurityDto.builder()
                .webcamMonitoring(exam.isWebcamMonitoring())
                .tabSwitchTracking(exam.isTabSwitchTracking())
                .blockCopyPaste(exam.isBlockCopyPaste())
                .build();
    }

    private ExamDetailBadgeDto buildDetail(Exam exam, ExamDisplayStatus status, ExamSecurityDto security) {
        return switch (status) {
            case LIVE -> exam.getActiveFlagsCount() > 0
                    ? ExamDetailBadgeDto.builder()
                    .type("flags")
                    .label(exam.getActiveFlagsCount() + " Active Flags")
                    .icon("warning")
                    .tone("error")
                    .build()
                    : null;
            case UPCOMING -> isFullyConfigured(security)
                    ? ExamDetailBadgeDto.builder()
                    .type("draft")
                    .label("Draft Ready")
                    .icon("verified")
                    .tone("muted")
                    .build()
                    : ExamDetailBadgeDto.builder()
                    .type("config")
                    .label("Needs Configuration")
                    .icon("pending")
                    .tone("secondary")
                    .build();
            case COMPLETED -> ExamDetailBadgeDto.builder()
                    .type("draft")
                    .label("Graded")
                    .icon("task_alt")
                    .tone("muted")
                    .build();
        };
    }

    private boolean isFullyConfigured(ExamSecurityDto security) {
        return security.isWebcamMonitoring()
                && security.isTabSwitchTracking()
                && security.isBlockCopyPaste();
    }

    private ExamDisplayStatus computeDisplayStatus(Exam exam) {
        return ExamDisplayStatusResolver.resolve(exam);
    }

    private int resolveDurationMinutes(Exam exam) {
        return ExamDisplayStatusResolver.resolveDurationMinutes(exam);
    }

    private String formatCourseLabel(String courseCode, String courseName) {
        if (courseCode == null || courseCode.isBlank()) {
            return courseName;
        }
        return courseCode + ": " + courseName;
    }

    private String formatTerm(LocalDateTime startLocal) {
        Month month = startLocal.getMonth();
        int year = startLocal.getYear();
        String season = switch (month) {
            case DECEMBER, JANUARY, FEBRUARY, MARCH, APRIL, MAY -> "Spring";
            case JUNE, JULY, AUGUST -> "Summer";
            default -> "Fall";
        };
        return season + " " + year;
    }

    private String formatSchedule(LocalDateTime start, LocalDateTime end) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = start.toLocalDate();
        String timeRange = TIME_FORMAT.format(start) + " - " + TIME_FORMAT.format(end);

        if (startDate.equals(today)) {
            return "Today, " + timeRange;
        }
        if (startDate.equals(today.plusDays(1))) {
            return "Tomorrow, " + timeRange;
        }
        return DATE_FORMAT.format(startDate) + " • " + timeRange;
    }

    private String formatEnrollment(Exam exam, ExamDisplayStatus status) {
        return switch (status) {
            case LIVE -> {
                if (exam.getCapacityCount() != null) {
                    yield exam.getEnrolledCount() + " / " + exam.getCapacityCount() + " Students Joined";
                }
                yield exam.getEnrolledCount() + " Students Joined";
            }
            case UPCOMING -> exam.getEnrolledCount() + " Enrolled";
            case COMPLETED -> exam.getEnrolledCount() + " Completed";
        };
    }

    private Instant parseStartAt(String startAt) {
        try {
            LocalDateTime localDateTime = LocalDateTime.parse(startAt, ISO_LOCAL);
            return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
        } catch (DateTimeParseException ex) {
            try {
                return Instant.parse(startAt);
            } catch (DateTimeParseException ignored) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid startAt format");
            }
        }
    }

    private ParsedCourse parseCourse(String course) {
        String trimmed = course.trim();
        if (trimmed.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Course is required");
        }

        int colonIndex = trimmed.indexOf(':');
        if (colonIndex > 0) {
            String code = trimmed.substring(0, colonIndex).trim();
            String name = trimmed.substring(colonIndex + 1).trim();
            if (name.isEmpty()) {
                name = code;
            }
            return new ParsedCourse(code, name);
        }

        String slug = trimmed.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "");
        String code = slug.isEmpty() ? "COURSE" : slug.substring(0, Math.min(slug.length(), 8));
        return new ParsedCourse(code, trimmed);
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    private boolean matchesStatusFilter(String status, String statusFilter) {
        if (statusFilter == null || statusFilter.isBlank() || "ALL".equalsIgnoreCase(statusFilter)) {
            return true;
        }
        return status.equalsIgnoreCase(statusFilter);
    }

    private boolean matchesSearch(LecturerExamDto dto, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }
        String needle = search.trim().toLowerCase(Locale.ROOT);
        return containsIgnoreCase(dto.getTitle(), needle)
                || containsIgnoreCase(dto.getCourseCode(), needle)
                || containsIgnoreCase(dto.getCourseName(), needle)
                || containsIgnoreCase(dto.getTerm(), needle);
    }

    private boolean containsIgnoreCase(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }

    private Comparator<LecturerExamDto> examListComparator() {
        return Comparator
                .comparingInt((LecturerExamDto dto) -> statusRank(dto.getStatus()))
                .thenComparing(LecturerExamDto::getStartAt, Comparator.reverseOrder());
    }

    private int statusRank(String status) {
        return switch (status) {
            case "LIVE" -> 0;
            case "UPCOMING" -> 1;
            case "COMPLETED" -> 2;
            default -> 3;
        };
    }

    private record ParsedCourse(String code, String name) {}
}
