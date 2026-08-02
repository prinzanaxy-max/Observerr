package com.backend.observerr.exam;

import com.backend.observerr.auth.model.*;
import com.backend.observerr.auth.service.JwtService;
import com.backend.observerr.exam.dto.ExamQuestionRequest;
import com.backend.observerr.exam.model.*;
import com.backend.observerr.exam.repository.*;
import com.backend.observerr.exam.service.ExamQuestionService;
import com.backend.observerr.integrity.model.*;
import com.backend.observerr.integrity.repository.ExamSessionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ExamCoreIntegrationTest {

    @Autowired private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired private UserRepository userRepository;
    @Autowired private ExamRepository examRepository;
    @Autowired private ExamEnrollmentRepository enrollmentRepository;
    @Autowired private ExamSessionRepository sessionRepository;
    @Autowired private ExamQuestionService questionService;
    @Autowired private ExamAnswerRepository answerRepository;
    @Autowired private ExamResultRepository resultRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    private User lecturer;
    private User student;
    private Exam exam;
    private UUID sessionId;
    private String lecturerToken;
    private String studentToken;

    @BeforeEach
    void setUp() {
        lecturer = userRepository.save(User.builder()
                .institutionalId("LEC-CORE-1")
                .email("lecturer-core@test.com")
                .firstName("Core")
                .lastName("Lecturer")
                .password(passwordEncoder.encode("password"))
                .role(Role.LECTURER)
                .build());
        student = userRepository.save(User.builder()
                .institutionalId("STU-CORE-1")
                .email("student-core@test.com")
                .firstName("Core")
                .lastName("Student")
                .password(passwordEncoder.encode("password"))
                .role(Role.STUDENT)
                .build());

        exam = examRepository.save(Exam.builder()
                .title("Production MCQ")
                .lecturerId(lecturer.getId())
                .courseCode("CS500")
                .courseName("Production Systems")
                .durationMinutes(60)
                .status(ExamStatus.LIVE)
                .startTime(Instant.now().minusSeconds(60))
                .endTime(Instant.now().plusSeconds(3600))
                .published(true)
                .build());
        enrollmentRepository.save(ExamEnrollment.builder()
                .examId(exam.getId())
                .studentId(student.getId())
                .build());

        questionService.createQuestions(exam.getId(), List.of(
                question("Correct is B", 2, AnswerChoice.B),
                question("Correct is D", 3, AnswerChoice.D)));

        sessionId = UUID.randomUUID();
        sessionRepository.save(ExamSession.builder()
                .id(sessionId)
                .examId(exam.getId())
                .studentId(student.getId())
                .startedAt(Instant.now().minusSeconds(30))
                .startingScore((short) 100)
                .status(ExamSessionStatus.IN_PROGRESS)
                .proctoringAvailable(true)
                .build());

        lecturerToken = jwtService.generateAccessToken(lecturer);
        studentToken = jwtService.generateAccessToken(student);
    }

    @Test
    void questionsAutosaveGradeAndReleaseAreSecureAndIdempotent() throws Exception {
        Long firstQuestionId = questionService.loadQuestions(exam.getId()).getFirst().getId();

        mockMvc.perform(get("/api/student/exams/{examId}/questions", exam.getId())
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].options", hasSize(4)))
                .andExpect(jsonPath("$[0].correctAnswer").doesNotExist());

        String answerBody = objectMapper.writeValueAsString(Map.of("selectedOption", "B"));
        mockMvc.perform(put("/api/student/exam-sessions/{sessionId}/answers/{questionId}",
                        sessionId, firstQuestionId)
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(answerBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selectedOption").value("B"))
                .andExpect(jsonPath("$.submitted").value(false));

        mockMvc.perform(get("/api/student/exam-sessions/{sessionId}/answers", sessionId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].selectedOption").value("B"));

        String completion = completionBody();
        complete(completion);
        complete(completion);

        assertEquals(1, resultRepository.count());
        assertEquals(1, answerRepository.count());
        ExamResult pending = resultRepository.findBySessionId(sessionId).orElseThrow();
        assertEquals(2, pending.getAcademicScore());
        assertEquals(5, pending.getMaxScore());
        assertEquals(ExamResultStatus.PENDING, pending.getReleaseStatus());

        mockMvc.perform(get("/api/student/results")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/api/lecturer/exams/{examId}/results", exam.getId())
                        .header("Authorization", "Bearer " + lecturerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].academicScore").value(2))
                .andExpect(jsonPath("$[0].status").value("PENDING"));

        mockMvc.perform(post("/api/lecturer/exams/{examId}/results/release", exam.getId())
                        .header("Authorization", "Bearer " + lecturerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("RELEASED"));

        mockMvc.perform(get("/api/student/results/{resultId}", pending.getId())
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.percentage").value(40.0))
                .andExpect(jsonPath("$.analysis", hasSize(2)))
                .andExpect(jsonPath("$.analysis[0].correctAnswer").value("B"))
                .andExpect(jsonPath("$.analysis[0].correct").value(true))
                .andExpect(jsonPath("$.analysis[1].selectedAnswer").value(nullValue()));

        mockMvc.perform(post("/api/lecturer/exams/{examId}/results/unrelease", exam.getId())
                        .header("Authorization", "Bearer " + lecturerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/student/results/{resultId}", pending.getId())
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void lecturerReplacesExplicitEnrollmentAndRbacIsEnforced() throws Exception {
        User intendedStudent = userRepository.save(User.builder()
                .institutionalId("STU-CORE-2")
                .email("student-core-2@test.com")
                .password(passwordEncoder.encode("password"))
                .role(Role.STUDENT)
                .build());
        String intendedStudentToken = jwtService.generateAccessToken(intendedStudent);
        String body = objectMapper.writeValueAsString(Map.of(
                "studentInstitutionalIds", List.of(intendedStudent.getInstitutionalId())));

        mockMvc.perform(put("/api/lecturer/exams/{examId}/enrollments", exam.getId())
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/lecturer/exams/{examId}/enrollments", exam.getId())
                        .header("Authorization", "Bearer " + lecturerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enrolledCount").value(1))
                .andExpect(jsonPath("$.studentInstitutionalIds[0]")
                        .value(intendedStudent.getInstitutionalId()));

        mockMvc.perform(get("/api/student/exams")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/api/student/exams")
                        .header("Authorization", "Bearer " + intendedStudentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.exams[0].id").value(exam.getId()));
    }

    private void complete(String completion) throws Exception {
        mockMvc.perform(post("/api/student/exam-sessions/{sessionId}/complete", sessionId)
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(completion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    private String completionBody() throws Exception {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("sessionId", sessionId.toString());
        summary.put("examId", exam.getId());
        summary.put("startedAt", Instant.now().minusSeconds(30).toString());
        summary.put("endedAt", Instant.now().toString());
        summary.put("startingScore", 100);
        summary.put("finalScore", 100);
        summary.put("totalEvents", 0);
        summary.put("totalDeductions", 0);
        summary.put("requiresReview", false);
        summary.put("proctoringAvailable", true);
        return objectMapper.writeValueAsString(Map.of("summary", summary, "events", List.of()));
    }

    private ExamQuestionRequest question(String text, int points, AnswerChoice correct) {
        ExamQuestionRequest request = new ExamQuestionRequest();
        request.setText(text);
        request.setPoints(points);
        request.setCorrectAnswer(correct);
        request.setOptions(Map.of(
                AnswerChoice.A, text + " A",
                AnswerChoice.B, text + " B",
                AnswerChoice.C, text + " C",
                AnswerChoice.D, text + " D"));
        return request;
    }
}
