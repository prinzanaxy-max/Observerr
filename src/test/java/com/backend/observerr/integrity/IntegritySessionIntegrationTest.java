package com.backend.observerr.integrity;

import com.backend.observerr.auth.model.Role;
import com.backend.observerr.auth.model.User;
import com.backend.observerr.auth.model.UserRepository;
import com.backend.observerr.auth.service.JwtService;
import com.backend.observerr.exam.model.Exam;
import com.backend.observerr.exam.model.ExamEnrollment;
import com.backend.observerr.exam.model.ExamStatus;
import com.backend.observerr.exam.repository.ExamEnrollmentRepository;
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
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.time.Instant;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    private ExamEnrollmentRepository examEnrollmentRepository;

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
                .status(ExamStatus.LIVE)
                .startTime(Instant.now().minusSeconds(60))
                .endTime(Instant.now().plusSeconds(7200))
                .durationMinutes(120)
                .published(true)
                .build());

        examEnrollmentRepository.save(ExamEnrollment.builder()
                .examId(exam.getId())
                .studentId(student.getId())
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
                    "finalScore": 70,
                    "totalEvents": 1,
                    "totalDeductions": 30,
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
                .andExpect(jsonPath("$.finalScore").value(70))
                .andExpect(jsonPath("$.requiresReview").value(true))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(get("/api/lecturer/students/sessions/{sessionId}", sessionId)
                        .header("Authorization", "Bearer " + lecturerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(sessionId))
                .andExpect(jsonPath("$.integrityScore").value(70))
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

        mockMvc.perform(post("/api/student/exam-sessions/{sessionId}/media-token", sessionId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isConflict());
    }

    @Test
    void mediaTokensContainRoomScopedLeastPrivilegeGrants() throws Exception {
        String sessionId = startSession(studentToken);

        String studentResponse = mockMvc.perform(post(
                        "/api/student/exam-sessions/{sessionId}/media-token", sessionId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomName").value("exam-" + exam.getId()))
                .andExpect(jsonPath("$.participantIdentity").value(sessionId))
                .andReturn().getResponse().getContentAsString();

        JsonNode studentClaims = decodeClaims(objectMapper.readTree(studentResponse).get("token").asText());
        assertEquals(sessionId, studentClaims.get("sub").asText());
        assertEquals("exam-" + exam.getId(), studentClaims.at("/video/room").asText());
        assertTrue(studentClaims.at("/video/canPublish").asBoolean());
        assertFalse(studentClaims.at("/video/canSubscribe").asBoolean());
        assertEquals("camera", studentClaims.at("/video/canPublishSources/0").asText());
        assertEquals("microphone", studentClaims.at("/video/canPublishSources/1").asText());

        String lecturerResponse = mockMvc.perform(post(
                        "/api/lecturer/proctoring/exams/{examId}/media-token", exam.getId())
                        .header("Authorization", "Bearer " + lecturerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participantIdentity").value("lecturer-" + lecturer.getId()))
                .andReturn().getResponse().getContentAsString();

        JsonNode lecturerClaims = decodeClaims(objectMapper.readTree(lecturerResponse).get("token").asText());
        assertFalse(lecturerClaims.at("/video/canPublish").asBoolean());
        assertTrue(lecturerClaims.at("/video/canSubscribe").asBoolean());

        mockMvc.perform(post("/api/student/exam-sessions/{sessionId}/media-token", sessionId)
                        .header("Authorization", "Bearer " + otherStudentToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void serverCanonicalizesDeductionsAndRejectsUnknownCodes() throws Exception {
        String sessionId = startSession(studentToken);
        UUID eventId = UUID.randomUUID();
        postEvents(sessionId, studentToken, eventId, "GAZE_DEVIATION_BRIEF", 99, 1);

        mockMvc.perform(get("/api/lecturer/students/sessions/{sessionId}", sessionId)
                        .header("Authorization", "Bearer " + lecturerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.integrityScore").value(99))
                .andExpect(jsonPath("$.events[0].pointsDeducted").value(1))
                .andExpect(jsonPath("$.events[0].scoreAfter").value(99))
                .andExpect(jsonPath("$.events[0].severity").value("LOW"));

        mockMvc.perform(post("/api/student/exam-sessions/{sessionId}/integrity-events", sessionId)
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "events", java.util.List.of(buildEvent(
                                        UUID.randomUUID(), "MADE_UP_EVENT", 50, 0))
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void acceptsInformationalPartialFaceTransitionsWithoutChangingScore() throws Exception {
        String sessionId = startSession(studentToken);

        mockMvc.perform(post("/api/student/exam-sessions/{sessionId}/integrity-events", sessionId)
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "events", java.util.List.of(
                                        buildEvent(UUID.randomUUID(), "FACE_PARTIAL_DETECTED", 99, 1),
                                        buildEvent(UUID.randomUUID(), "FACE_PARTIAL_CLEARED", 99, 1))
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(2))
                .andExpect(jsonPath("$.currentScore").value(100))
                .andExpect(jsonPath("$.requiresReview").value(false));
    }

    @Test
    void unavailableProctoringCapsScoreAndForcesReview() throws Exception {
        String sessionId = startSession(studentToken);
        mockMvc.perform(post("/api/student/exam-sessions/{sessionId}/integrity-events", sessionId)
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "events", java.util.List.of(buildEvent(
                                        UUID.randomUUID(), "PROCTORING_UNAVAILABLE", 15, 85))
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentScore").value(85))
                .andExpect(jsonPath("$.requiresReview").value(true));

        String completeBody = """
                {
                  "summary": {
                    "sessionId": "%s",
                    "examId": %d,
                    "startedAt": "2026-07-26T10:00:00Z",
                    "endedAt": "2026-07-26T11:00:00Z",
                    "startingScore": 100,
                    "finalScore": 85,
                    "totalEvents": 1,
                    "totalDeductions": 15,
                    "requiresReview": true,
                    "proctoringAvailable": false
                  }
                }
                """.formatted(sessionId, exam.getId());

        mockMvc.perform(post("/api/student/exam-sessions/{sessionId}/complete", sessionId)
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(completeBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.finalScore").value(85))
                .andExpect(jsonPath("$.requiresReview").value(true));
    }

    @Test
    void rejectsTamperedCompletionSummary() throws Exception {
        String sessionId = startSession(studentToken);
        postEvents(sessionId, studentToken, UUID.randomUUID(), "TAB_BLUR", 0, 100);

        String completeBody = """
                {
                  "summary": {
                    "sessionId": "%s",
                    "examId": %d,
                    "startedAt": "2026-07-26T10:00:00Z",
                    "endedAt": "2026-07-26T11:00:00Z",
                    "startingScore": 100,
                    "finalScore": 100,
                    "totalEvents": 1,
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
                .andExpect(status().isBadRequest());
    }

    @Test
    void lecturerCannotStartAnotherLecturersExam() throws Exception {
        User otherLecturer = userRepository.save(User.builder()
                .institutionalId("LEC-INTEGRITY-2")
                .email("integrity-other-lecturer@test.com")
                .firstName("Other")
                .lastName("Lecturer")
                .password(passwordEncoder.encode("password"))
                .role(Role.LECTURER)
                .build());

        mockMvc.perform(post("/api/lecturer/exams/{examId}/start", exam.getId())
                        .header("Authorization", "Bearer " + jwtService.generateAccessToken(otherLecturer)))
                .andExpect(status().isNotFound());
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

    private JsonNode decodeClaims(String token) throws Exception {
        String payload = token.split("\\.")[1];
        return objectMapper.readTree(Base64.getUrlDecoder().decode(payload));
    }
}
