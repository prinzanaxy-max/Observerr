package com.backend.observerr.student.exams;

import com.backend.observerr.auth.model.Role;
import com.backend.observerr.auth.model.User;
import com.backend.observerr.auth.model.UserRepository;
import com.backend.observerr.auth.service.JwtService;
import com.backend.observerr.exam.model.Exam;
import com.backend.observerr.exam.model.ExamEnrollment;
import com.backend.observerr.exam.model.ExamStatus;
import com.backend.observerr.exam.repository.ExamEnrollmentRepository;
import com.backend.observerr.exam.repository.ExamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StudentExamIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private ExamEnrollmentRepository examEnrollmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User student;
    private String studentToken;
    private Exam exam;

    @BeforeEach
    void setUp() {
        User lecturer = userRepository.save(User.builder()
                .institutionalId("LEC-STU-EXAM-1")
                .email("student-exam-lecturer@test.com")
                .firstName("Lecturer")
                .lastName("One")
                .password(passwordEncoder.encode("password"))
                .role(Role.LECTURER)
                .build());

        student = userRepository.save(User.builder()
                .institutionalId("STU-EXAM-LIST-1")
                .email("student-exam-list@test.com")
                .firstName("Student")
                .lastName("One")
                .password(passwordEncoder.encode("password"))
                .role(Role.STUDENT)
                .build());

        exam = examRepository.save(Exam.builder()
                .title("Live Calculus Quiz")
                .lecturerId(lecturer.getId())
                .courseCode("MATH202")
                .courseName("Calculus")
                .status(ExamStatus.SCHEDULED)
                .startTime(Instant.now().minusSeconds(600))
                .durationMinutes(120)
                .published(true)
                .build());

        examEnrollmentRepository.save(ExamEnrollment.builder()
                .examId(exam.getId())
                .studentId(student.getId())
                .build());

        studentToken = jwtService.generateAccessToken(student);
    }

    @Test
    void listAndGetEnrolledExams() throws Exception {
        mockMvc.perform(get("/api/student/exams")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.exams", hasSize(1)))
                .andExpect(jsonPath("$.exams[0].id").value(exam.getId()))
                .andExpect(jsonPath("$.exams[0].status").value("LIVE"))
                .andExpect(jsonPath("$.exams[0].canTake").value(true));

        mockMvc.perform(get("/api/student/exams/{examId}", exam.getId())
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(exam.getId()))
                .andExpect(jsonPath("$.title").value("Live Calculus Quiz"))
                .andExpect(jsonPath("$.security.webcamMonitoring").value(true));
    }
}
