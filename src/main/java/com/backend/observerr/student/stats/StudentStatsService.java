package com.backend.observerr.student.stats;

import com.backend.observerr.auth.model.User;
import com.backend.observerr.exam.repository.ExamResultRepository;
import com.backend.observerr.exam.repository.ExamResultStatsProjection;
import com.backend.observerr.student.stats.dto.StudentStatsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudentStatsService {

    private final ExamResultRepository resultRepository;

    @Transactional(readOnly = true)
    public StudentStatsResponse getStats(User student) {
        ExamResultStatsProjection stats = resultRepository.summarizeStudent(student.getId());
        long completed = stats == null ? 0 : stats.getExamsCompleted();
        double average = completed == 0 || stats.getAverageIntegrity() == null
                ? 100
                : stats.getAverageIntegrity();
        return StudentStatsResponse.builder()
                .examsCompleted(Math.toIntExact(completed))
                .avgIntegrity((int) Math.round(average))
                .verifiedSessions(Math.toIntExact(stats == null ? 0 : stats.getVerifiedSessions()))
                .underReview(Math.toIntExact(stats == null ? 0 : stats.getUnderReview()))
                .build();
    }
}
