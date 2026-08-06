package com.backend.observerr.student.exams;

import com.backend.observerr.auth.model.User;
import com.backend.observerr.exam.ExamDisplayStatusResolver;
import com.backend.observerr.exam.dto.ExamSecurityDto;
import com.backend.observerr.exam.model.Exam;
import com.backend.observerr.exam.model.ExamDisplayStatus;
import com.backend.observerr.exam.repository.ExamRepository;
import com.backend.observerr.student.exams.dto.StudentExamDto;
import com.backend.observerr.student.exams.dto.StudentExamListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class StudentExamService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a", Locale.US);
    private static final DateTimeFormatter ISO_LOCAL = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final ExamRepository examRepository;

    @Transactional(readOnly = true)
    public StudentExamListResponse listExams(User student) {
        List<StudentExamDto> exams = examRepository.findPublishedExamsForStudent(student.getId())
                .stream()
                .map(this::toDto)
                .sorted(examListComparator())
                .toList();

        return StudentExamListResponse.builder()
                .exams(exams)
                .totalElements(exams.size())
                .build();
    }

    @Transactional(readOnly = true)
    public StudentExamDto getExam(User student, Long examId) {
        Exam exam = examRepository.findPublishedExamForStudent(examId, student.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam not found"));
        return toDto(exam);
    }

    private StudentExamDto toDto(Exam exam) {
        ExamDisplayStatus displayStatus = computeDisplayStatus(exam);
        LocalDateTime startLocal = toLocalDateTime(exam.getStartTime());
        LocalDateTime endLocal = startLocal.plusMinutes(resolveDurationMinutes(exam));

        return StudentExamDto.builder()
                .id(exam.getId())
                .title(exam.getTitle())
                .courseCode(exam.getCourseCode())
                .courseName(exam.getCourseName())
                .courseLabel(formatCourseLabel(exam.getCourseCode(), exam.getCourseName()))
                .schedule(formatSchedule(startLocal, endLocal))
                .status(displayStatus.name())
                .startAt(startLocal.format(ISO_LOCAL))
                .endAt(endLocal.format(ISO_LOCAL))
                .durationMinutes(resolveDurationMinutes(exam))
                .security(toSecurityDto(exam))
                .canTake(displayStatus == ExamDisplayStatus.LIVE)
                .build();
    }

    private ExamSecurityDto toSecurityDto(Exam exam) {
        return ExamSecurityDto.builder()
                .webcamMonitoring(exam.isWebcamMonitoring())
                .tabSwitchTracking(exam.isTabSwitchTracking())
                .blockCopyPaste(exam.isBlockCopyPaste())
                .build();
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

    private LocalDateTime toLocalDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    private Comparator<StudentExamDto> examListComparator() {
        return Comparator
                .comparingInt((StudentExamDto dto) -> statusRank(dto.getStatus()))
                .thenComparing(StudentExamDto::getStartAt, Comparator.reverseOrder());
    }

    private int statusRank(String status) {
        return switch (status) {
            case "LIVE" -> 0;
            case "UPCOMING" -> 1;
            case "COMPLETED" -> 2;
            default -> 3;
        };
    }
}
