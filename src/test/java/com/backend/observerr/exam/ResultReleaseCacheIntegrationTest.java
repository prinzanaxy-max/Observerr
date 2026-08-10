package com.backend.observerr.exam;

import com.backend.observerr.auth.model.Role;
import com.backend.observerr.auth.model.User;
import com.backend.observerr.auth.model.UserRepository;
import com.backend.observerr.config.CacheConfig;
import com.backend.observerr.exam.model.Exam;
import com.backend.observerr.exam.model.ExamResult;
import com.backend.observerr.exam.model.ExamResultStatus;
import com.backend.observerr.exam.model.ExamStatus;
import com.backend.observerr.exam.repository.ExamRepository;
import com.backend.observerr.exam.repository.ExamResultRepository;
import com.backend.observerr.exam.service.ExamAttemptService;
import com.backend.observerr.integrity.model.ExamSession;
import com.backend.observerr.integrity.model.ExamSessionStatus;
import com.backend.observerr.integrity.repository.ExamSessionRepository;
import com.backend.observerr.notification.NotificationInboxService;
import com.backend.observerr.notification.dto.NotificationPreferencesDto;
import com.backend.observerr.notification.repository.UserNotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ResultReleaseCacheIntegrationTest {

    @Autowired private UserRepository userRepository;
    @Autowired private ExamRepository examRepository;
    @Autowired private ExamSessionRepository sessionRepository;
    @Autowired private ExamResultRepository resultRepository;
    @Autowired private UserNotificationRepository notificationRepository;
    @Autowired private NotificationInboxService inboxService;
    @Autowired private ExamAttemptService attemptService;
    @Autowired private CacheManager cacheManager;

    @Test
    void releaseAndUnreleaseEvictCachesAndHonorResultNotificationPreference() {
        User lecturer = userRepository.save(user("LEC-CACHE-1", "lec-cache@test.com", Role.LECTURER));
        User student = userRepository.save(user("STU-CACHE-1", "stu-cache@test.com", Role.STUDENT));
        Exam exam = examRepository.save(Exam.builder()
                .title("Cache exam").lecturerId(lecturer.getId())
                .courseCode("CACHE").courseName("Caching")
                .durationMinutes(60).status(ExamStatus.ENDED)
                .startTime(Instant.now().minusSeconds(7200))
                .endTime(Instant.now().minusSeconds(3600))
                .published(true).build());
        UUID sessionId = UUID.randomUUID();
        sessionRepository.save(ExamSession.builder()
                .id(sessionId).examId(exam.getId()).studentId(student.getId())
                .startedAt(Instant.now().minusSeconds(7200)).endedAt(Instant.now().minusSeconds(3600))
                .startingScore((short) 100).finalScore((short) 95)
                .status(ExamSessionStatus.COMPLETED).proctoringAvailable(true).build());
        ExamResult result = resultRepository.save(ExamResult.builder()
                .examId(exam.getId()).sessionId(sessionId).studentId(student.getId())
                .lecturerId(lecturer.getId()).academicScore(8).maxScore(10)
                .integrityScore((short) 95).releaseStatus(ExamResultStatus.PENDING)
                .submittedAt(Instant.now().minusSeconds(3600)).build());

        updatePreferences(student.getId(), false);
        attemptService.studentResults(student, 0, 10, "MOST_RECENT");
        String pageKey = "exam:" + student.getId() + ":0:10:MOST_RECENT";
        assertThat(cacheManager.getCache(CacheConfig.STUDENT_RESULTS_PAGE_CACHE).get(pageKey)).isNotNull();

        attemptService.setReleased(lecturer, exam.getId(), List.of(result.getId()), true);

        assertThat(cacheManager.getCache(CacheConfig.STUDENT_RESULTS_PAGE_CACHE).get(pageKey)).isNull();
        assertThat(notificationRepository.findByUserIdAndDeduplicationKey(
                student.getId(), "result-release:" + result.getId())).isEmpty();

        updatePreferences(student.getId(), true);
        attemptService.setReleased(lecturer, exam.getId(), List.of(result.getId()), false);
        attemptService.setReleased(lecturer, exam.getId(), List.of(result.getId()), true);
        attemptService.studentResultDetail(student, result.getId());
        String detailKey = student.getId() + ":" + result.getId();
        assertThat(cacheManager.getCache(CacheConfig.STUDENT_RESULT_DETAIL_CACHE).get(detailKey)).isNotNull();
        assertThat(notificationRepository.findByUserIdAndDeduplicationKey(
                student.getId(), "result-release:" + result.getId())).isPresent();

        attemptService.setReleased(lecturer, exam.getId(), List.of(result.getId()), false);

        assertThat(cacheManager.getCache(CacheConfig.STUDENT_RESULT_DETAIL_CACHE).get(detailKey)).isNull();
        assertThat(resultRepository.findById(result.getId()).orElseThrow().getReleaseStatus())
                .isEqualTo(ExamResultStatus.PENDING);
    }

    private void updatePreferences(Long userId, boolean resultUpdates) {
        inboxService.updatePreferences(userId, NotificationPreferencesDto.builder()
                .examEvents(true).integrityAlerts(true)
                .resultUpdates(resultUpdates).systemUpdates(true).build());
    }

    private User user(String institutionalId, String email, Role role) {
        return User.builder()
                .institutionalId(institutionalId).email(email)
                .password("not-used").role(role).build();
    }
}
