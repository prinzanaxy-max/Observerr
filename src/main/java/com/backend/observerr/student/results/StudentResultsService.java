package com.backend.observerr.student.results;

import com.backend.observerr.auth.model.User;
import com.backend.observerr.config.CacheConfig;
import com.backend.observerr.student.results.dto.CompletedAssessmentDto;
import com.backend.observerr.student.results.dto.CompletedAssessmentTimingDto;
import com.backend.observerr.student.results.dto.CompletedAssessmentsPageDto;
import com.backend.observerr.student.results.dto.CompletedAssessmentsSummaryDto;
import com.backend.observerr.student.results.model.AssessmentResultStatus;
import com.backend.observerr.student.results.model.AssessmentTimingType;
import com.backend.observerr.student.results.model.ResultsSortOption;
import com.backend.observerr.student.results.model.StudentCompletedAssessment;
import com.backend.observerr.student.results.repository.StudentCompletedAssessmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class StudentResultsService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("hh:mm a", Locale.US);

    private final StudentCompletedAssessmentRepository repository;

    @Transactional(readOnly = true)
    @Cacheable(
            value = CacheConfig.STUDENT_RESULTS_PAGE_CACHE,
            key = "#user.id + ':' + #page + ':' + #size + ':' + #sort.name()"
    )
    public CompletedAssessmentsPageDto getCompletedAssessments(
            User user,
            int page,
            int size,
            ResultsSortOption sort) {
        Pageable pageable = PageRequest.of(page, size, toSort(sort));
        Page<StudentCompletedAssessment> results = repository.findByStudentId(user.getId(), pageable);

        List<CompletedAssessmentDto> content = results.getContent().stream()
                .map(this::toDto)
                .toList();

        int from = results.isEmpty() ? 0 : (page * size) + 1;
        int to = results.isEmpty() ? 0 : from + results.getNumberOfElements() - 1;

        return CompletedAssessmentsPageDto.builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(results.getTotalElements())
                .totalPages(results.getTotalPages())
                .sort(sort.name())
                .from(from)
                .to(to)
                .build();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.STUDENT_RESULTS_SUMMARY_CACHE, key = "#user.id")
    public CompletedAssessmentsSummaryDto getSummary(User user) {
        long total = repository.countByStudentId(user.getId());
        long verified = repository.countByStudentIdAndStatus(user.getId(), AssessmentResultStatus.VERIFIED);
        long underReview = repository.countByStudentIdAndStatus(user.getId(), AssessmentResultStatus.UNDER_REVIEW);
        int avgIntegrity = total == 0 ? 0 : (int) Math.round(repository.averageIntegrityScoreByStudentId(user.getId()));

        return CompletedAssessmentsSummaryDto.builder()
                .examsCompleted(total)
                .avgIntegrity(avgIntegrity)
                .verifiedSessions(verified)
                .underReview(underReview)
                .build();
    }

    private Sort toSort(ResultsSortOption sort) {
        return switch (sort) {
            case OLDEST -> Sort.by(Sort.Direction.ASC, "takenDate").and(Sort.by(Sort.Direction.ASC, "id"));
            case HIGHEST_INTEGRITY -> Sort.by(Sort.Direction.DESC, "integrityScore").and(Sort.by(Sort.Direction.DESC, "takenDate"));
            case LOWEST_INTEGRITY -> Sort.by(Sort.Direction.ASC, "integrityScore").and(Sort.by(Sort.Direction.DESC, "takenDate"));
            case MOST_RECENT -> Sort.by(Sort.Direction.DESC, "takenDate").and(Sort.by(Sort.Direction.DESC, "id"));
        };
    }

    private CompletedAssessmentDto toDto(StudentCompletedAssessment assessment) {
        return CompletedAssessmentDto.builder()
                .id(assessment.getId())
                .courseName(assessment.getCourseName())
                .courseCode(assessment.getCourseCode())
                .assessmentType(assessment.getAssessmentType())
                .category(assessment.getCategory())
                .dateTaken(DATE_FORMAT.format(assessment.getTakenDate()))
                .timing(toTimingDto(assessment))
                .integrityScore(assessment.getIntegrityScore())
                .status(assessment.getStatus().name())
                .build();
    }

    private CompletedAssessmentTimingDto toTimingDto(StudentCompletedAssessment assessment) {
        if (assessment.getTimingType() == AssessmentTimingType.SUBMITTED) {
            return CompletedAssessmentTimingDto.builder()
                    .type("SUBMITTED")
                    .submittedTime(assessment.getSubmittedTime() != null
                            ? TIME_FORMAT.format(assessment.getSubmittedTime())
                            : null)
                    .build();
        }

        return CompletedAssessmentTimingDto.builder()
                .type("TIMED")
                .startTime(assessment.getStartTime() != null
                        ? TIME_FORMAT.format(assessment.getStartTime())
                        : null)
                .endTime(assessment.getEndTime() != null
                        ? TIME_FORMAT.format(assessment.getEndTime())
                        : null)
                .build();
    }
}
