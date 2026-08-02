package com.backend.observerr.exam;

import com.backend.observerr.auth.model.Role;
import com.backend.observerr.auth.model.User;
import com.backend.observerr.auth.model.UserRepository;
import com.backend.observerr.auth.service.JwtService;
import com.backend.observerr.exam.model.Exam;
import com.backend.observerr.exam.model.ExamEnrollment;
import com.backend.observerr.exam.model.ExamStatus;
import com.backend.observerr.exam.repository.ExamEnrollmentRepository;
import com.backend.observerr.exam.repository.ExamRepository;
import com.backend.observerr.integrity.model.ExamSession;
import com.backend.observerr.integrity.model.ExamSessionStatus;
import com.backend.observerr.integrity.repository.ExamSessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StudentBlockEnforcementIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private ExamRepository examRepository;
    @Autowired private ExamEnrollmentRepository enrollmentRepository;
    @Autowired private ExamSessionRepository sessionRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    @Test
    void blockTerminatesSessionAndUnblockRestoresAccess() throws Exception {
        User lecturer = userRepository.save(user("LEC-BLOCK-1", "lec-block@test.com", Role.LECTURER));
        User student = userRepository.save(user("STU-BLOCK-1", "stu-block@test.com", Role.STUDENT));
        Exam exam = examRepository.save(Exam.builder()
                .title("Blocking exam").lecturerId(lecturer.getId())
                .courseCode("BLOCK").courseName("Enforcement")
                .durationMinutes(60).status(ExamStatus.LIVE)
                .startTime(Instant.now().minusSeconds(60))
                .endTime(Instant.now().plusSeconds(3600))
                .published(true).webcamMonitoring(true).enrolledCount(1).build());
        enrollmentRepository.save(ExamEnrollment.builder()
                .examId(exam.getId()).studentId(student.getId()).build());
        UUID sessionId = UUID.randomUUID();
        sessionRepository.save(ExamSession.builder()
                .id(sessionId).examId(exam.getId()).studentId(student.getId())
                .startedAt(Instant.now().minusSeconds(30)).startingScore((short) 100)
                .status(ExamSessionStatus.IN_PROGRESS).proctoringAvailable(true).build());
        String lecturerJwt = jwtService.generateAccessToken(lecturer);
        String studentJwt = jwtService.generateAccessToken(student);

        mockMvc.perform(post("/api/lecturer/exams/{examId}/students/{studentId}/block",
                        exam.getId(), student.getInstitutionalId())
                        .header("Authorization", "Bearer " + lecturerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"policy violation\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blocked").value(true));

        ExamSession terminated = sessionRepository.findById(sessionId).orElseThrow();
        assertThat(terminated.getStatus()).isEqualTo(ExamSessionStatus.COMPLETED);
        assertThat(terminated.getEndedAt()).isNotNull();
        assertThat(terminated.isRequiresReview()).isTrue();

        mockMvc.perform(get("/api/student/exam-sessions/{sessionId}/answers", sessionId)
                        .header("Authorization", "Bearer " + studentJwt))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/student/exams/{examId}/sessions", exam.getId())
                        .header("Authorization", "Bearer " + studentJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/lecturer/exams/{examId}/students/{studentId}/unblock",
                        exam.getId(), student.getInstitutionalId())
                        .header("Authorization", "Bearer " + lecturerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blocked").value(false));
        mockMvc.perform(post("/api/student/exams/{examId}/sessions", exam.getId())
                        .header("Authorization", "Bearer " + studentJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    private User user(String institutionalId, String email, Role role) {
        return User.builder()
                .institutionalId(institutionalId).email(email)
                .password(passwordEncoder.encode("password")).role(role).build();
    }
}
