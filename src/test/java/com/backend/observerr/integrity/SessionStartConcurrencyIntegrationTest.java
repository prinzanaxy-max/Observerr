package com.backend.observerr.integrity;

import com.backend.observerr.auth.model.Role;
import com.backend.observerr.auth.model.User;
import com.backend.observerr.auth.model.UserRepository;
import com.backend.observerr.exam.model.Exam;
import com.backend.observerr.exam.model.ExamEnrollment;
import com.backend.observerr.exam.model.ExamStatus;
import com.backend.observerr.exam.repository.ExamEnrollmentRepository;
import com.backend.observerr.exam.repository.ExamRepository;
import com.backend.observerr.integrity.dto.StartExamSessionRequest;
import com.backend.observerr.integrity.model.ExamSessionStatus;
import com.backend.observerr.integrity.repository.ExamSessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SessionStartConcurrencyIntegrationTest {

    @Autowired private UserRepository userRepository;
    @Autowired private ExamRepository examRepository;
    @Autowired private ExamEnrollmentRepository enrollmentRepository;
    @Autowired private ExamSessionRepository sessionRepository;
    @Autowired private IntegritySessionService sessionService;

    @Test
    void concurrentStartsCreateOnlyOneActiveSession() throws Exception {
        User lecturer = userRepository.save(User.builder()
                .institutionalId("LEC-CONCURRENT-1").email("lec-concurrent@test.com")
                .password("not-used").role(Role.LECTURER).build());
        User student = userRepository.save(User.builder()
                .institutionalId("STU-CONCURRENT-1").email("stu-concurrent@test.com")
                .password("not-used").role(Role.STUDENT).build());
        Exam exam = examRepository.save(Exam.builder()
                .title("Concurrent start").lecturerId(lecturer.getId())
                .courseCode("LOCK").courseName("Locking")
                .durationMinutes(60).status(ExamStatus.LIVE)
                .startTime(Instant.now().minusSeconds(60))
                .endTime(Instant.now().plusSeconds(3600))
                .published(true).webcamMonitoring(true).enrolledCount(1).build());
        enrollmentRepository.save(ExamEnrollment.builder()
                .examId(exam.getId()).studentId(student.getId()).build());

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            List<Future<String>> attempts = List.of(
                    executor.submit(() -> startSession(ready, start, student, exam.getId())),
                    executor.submit(() -> startSession(ready, start, student, exam.getId())));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            assertThat(List.of(
                    attempts.get(0).get(10, TimeUnit.SECONDS),
                    attempts.get(1).get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("CREATED", "CONFLICT");
        }

        assertThat(sessionRepository.existsByExamIdAndStudentIdAndStatus(
                exam.getId(), student.getId(), ExamSessionStatus.IN_PROGRESS)).isTrue();
        assertThat(sessionRepository.findByExamId(exam.getId())).hasSize(1);
    }

    private String startSession(
            CountDownLatch ready, CountDownLatch start, User student, Long examId) throws Exception {
        ready.countDown();
        assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
        try {
            sessionService.startSession(student, examId, new StartExamSessionRequest());
            return "CREATED";
        } catch (ResponseStatusException ex) {
            assertThat(ex.getStatusCode().value()).isEqualTo(409);
            return "CONFLICT";
        }
    }
}
