package com.backend.observerr.integrity;

import com.backend.observerr.auth.model.Role;
import com.backend.observerr.auth.model.User;
import com.backend.observerr.auth.model.UserRepository;
import com.backend.observerr.auth.service.JwtService;
import com.backend.observerr.exam.model.Exam;
import com.backend.observerr.exam.repository.ExamRepository;
import com.fasterxml.jackson.databind.JsonNode;
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class IntegritySessionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User student;
    private User otherStudent;
    private User lecturer;
    private Exam exam;
    private String studentToken;
    private String otherStudentToken;
    private String lecturerToken;

    @BeforeEach
    void setUp() {
        lecturer = userRepository.save(User.builder()
                .institutionalId("LEC-INTEGRITY-1")
                .email("integrity-lecturer@test.com")
                .firstName("Jane")
                .lastName("Lecturer")
                .password(passwordEncoder.encode("password"))
                .role(Role.LECTURER)
                .build());

        student = userRepository.save(User.builder()
                .institutionalId("STU-INTEGRITY-1")
                .email("integrity-student@test.com")
                .firstName("Alex")
                .lastName("Student")
                .password(passwordEncoder.encode("password"))
                .role(Role.STUDENT)
                .build());

        otherStudent = userRepository.save(User.builder()
                .institutionalId("STU-INTEGRITY-2")
                .email("integrity-other@test.com")
                .firstName("Other")
                .lastName("Student")
                .password(passwordEncoder.encode("password"))
                .role(Role.STUDENT)
                .build());

        exam = examRepository.save(Exam.builder()
                .title("Integrity Midterm")
                .lecturerId(lecturer.getId())
                .courseCode("CS401")
                .courseName("Software Integrity")
                .build());

        studentToken = jwtService.generateAccessToken(student);
        otherStudentToken = jwtService.generateAccessToken(otherStudent);
        lecturerToken = jwtService.generateAccessToken(lecturer);
    }

    @Test
    void studentStartsSessionAndIngestsEvents() throws Exception {
        String sessionId = startSession(studentToken);

        UUID clientEventId = UUID.randomUUID();
        postEvents(sessionId, studentToken, clientEventId, "GAZE_DEVIATION_BRIEF", 1, 99);

        mockMvc.perform(post("/api/student/exam-sessions/{sessionId}/integrity-events", sessionId)
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "events", java.util.List.of(buildEvent(clientEventId, "GAZE_DEVIATION_BRIEF", 1, 99))
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(0))
                .andExpect(jsonPath("$.skipped").value(1));
    }

    @Test
    void completeSessionAndLecturerReadsTimeline() throws Exception {
        String sessionId = startSession(studentToken);
        UUID eventId = UUID.randomUUID();
        postEvents(sessionId, studentToken, eventId, "TAB_BLUR_NO_FACE", 30, 70);

        String completeBody = """
                {
                  "summary": {
                    "sessionId": "%s",
                    "examId": %d,
                    "startedAt": "2026-07-26T10:00:00Z",
                    "endedAt": "2026-07-26T11:30:00Z",
                    "startingScore": 100,
                    "finalScore": 42,
                    "totalEvents": 2,
                    "totalDeductions": 58,
                    "requiresReview": true,
                    "proctoringAvailable": true
                  },
                  "events": []
                }
                """.formatted(sessionId, exam.getId());

        mockMvc.perform(post("/api/student/exam-sessions/{sessionId}/complete", sessionId)
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(completeBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.finalScore").value(42))
                .andExpect(jsonPath("$.requiresReview").value(true))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(get("/api/lecturer/students/sessions/{sessionId}", sessionId)
                        .header("Authorization", "Bearer " + lecturerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(sessionId))
                .andExpect(jsonPath("$.integrityScore").value(42))
                .andExpect(jsonPath("$.requiresReview").value(true))
                .andExpect(jsonPath("$.events", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.events[0].eventCode").value("TAB_BLUR_NO_FACE"))
                .andExpect(jsonPath("$.events[0].pointsDeducted").value(30))
                .andExpect(jsonPath("$.events[0].scoreAfter").value(70));
    }

    @Test
    void studentCannotAccessOtherStudentsSessionTimeline() throws Exception {
        String sessionId = startSession(studentToken);

        mockMvc.perform(get("/api/lecturer/students/sessions/{sessionId}", sessionId)
                        .header("Authorization", "Bearer " + otherStudentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void lecturerCannotPostIntegrityEvents() throws Exception {
        String sessionId = startSession(studentToken);

        mockMvc.perform(post("/api/student/exam-sessions/{sessionId}/integrity-events", sessionId)
                        .header("Authorization", "Bearer " + lecturerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "events", java.util.List.of(buildEvent(UUID.randomUUID(), "TAB_BLUR", 2, 98))
                        ))))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsEventsAfterSessionCompleted() throws Exception {
        String sessionId = startSession(studentToken);

        String completeBody = """
                {
                  "summary": {
                    "sessionId": "%s",
                    "examId": %d,
                    "startedAt": "2026-07-26T10:00:00Z",
                    "endedAt": "2026-07-26T11:00:00Z",
                    "startingScore": 100,
                    "finalScore": 100,
                    "totalEvents": 0,
                    "totalDeductions": 0,
                    "requiresReview": false,
                    "proctoringAvailable": true
                  }
                }
                """.formatted(sessionId, exam.getId());

        mockMvc.perform(post("/api/student/exam-sessions/{sessionId}/complete", sessionId)
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(completeBody))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/student/exam-sessions/{sessionId}/integrity-events", sessionId)
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "events", java.util.List.of(buildEvent(UUID.randomUUID(), "TAB_BLUR", 2, 98))
                        ))))
                .andExpect(status().isConflict());
    }

    private String startSession(String token) throws Exception {
        String response = mockMvc.perform(post("/api/student/exams/{examId}/sessions", exam.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startingScore\":100}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode node = objectMapper.readTree(response);
        return node.get("sessionId").asText();
    }

    private void postEvents(
            String sessionId,
            String token,
            UUID clientEventId,
            String eventCode,
            int pointsDeducted,
            int scoreAfter) throws Exception {
        mockMvc.perform(post("/api/student/exam-sessions/{sessionId}/integrity-events", sessionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "events", java.util.List.of(buildEvent(clientEventId, eventCode, pointsDeducted, scoreAfter))
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(1))
                .andExpect(jsonPath("$.skipped").value(0));
    }

    private Map<String, Object> buildEvent(UUID clientEventId, String eventCode, int pointsDeducted, int scoreAfter) {
        Map<String, Object> event = new HashMap<>();
        event.put("clientEventId", clientEventId.toString());
        event.put("eventCode", eventCode);
        event.put("title", "Test event");
        event.put("description", "Integration test");
        event.put("severity", "CRITICAL");
        event.put("pointsDeducted", pointsDeducted);
        event.put("scoreAfter", scoreAfter);
        event.put("requiresReview", true);
        event.put("timestamp", "2026-07-26T10:05:12.345Z");
        event.put("durationMs", 3200);
        event.put("metadata", Map.of("rawType", "test", "examId", exam.getId()));
        return event;
    }
}
