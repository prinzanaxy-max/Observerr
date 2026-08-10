package com.backend.observerr.student.stats;

import com.backend.observerr.auth.model.User;
import com.backend.observerr.exam.repository.ExamResultRepository;
import com.backend.observerr.exam.repository.ExamResultStatsProjection;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StudentStatsServiceTest {

    private final ExamResultRepository repository = mock(ExamResultRepository.class);
    private final StudentStatsService service = new StudentStatsService(repository);

    @Test
    void defaultsIntegrityToOneHundredBeforeFirstCompletedExam() {
        User student = User.builder().id(7L).build();
        ExamResultStatsProjection projection = projection(0, null, 0, 0);
        when(repository.summarizeStudent(7L)).thenReturn(projection);

        var result = service.getStats(student);

        assertThat(result.getExamsCompleted()).isZero();
        assertThat(result.getAvgIntegrity()).isEqualTo(100);
        assertThat(result.getVerifiedSessions()).isZero();
        assertThat(result.getUnderReview()).isZero();
    }

    @Test
    void summarizesRealExamResults() {
        User student = User.builder().id(8L).build();
        ExamResultStatsProjection projection = projection(3, 86.6, 2, 1);
        when(repository.summarizeStudent(8L)).thenReturn(projection);

        var result = service.getStats(student);

        assertThat(result.getExamsCompleted()).isEqualTo(3);
        assertThat(result.getAvgIntegrity()).isEqualTo(87);
        assertThat(result.getVerifiedSessions()).isEqualTo(2);
        assertThat(result.getUnderReview()).isEqualTo(1);
    }

    private static ExamResultStatsProjection projection(
            long completed, Double average, long verified, long underReview) {
        ExamResultStatsProjection projection = mock(ExamResultStatsProjection.class);
        when(projection.getExamsCompleted()).thenReturn(completed);
        when(projection.getAverageIntegrity()).thenReturn(average);
        when(projection.getVerifiedSessions()).thenReturn(verified);
        when(projection.getUnderReview()).thenReturn(underReview);
        return projection;
    }
}
