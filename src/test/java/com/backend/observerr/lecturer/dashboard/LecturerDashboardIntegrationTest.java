package com.backend.observerr.lecturer.dashboard;

import com.backend.observerr.auth.model.Role;
import com.backend.observerr.auth.model.User;
import com.backend.observerr.auth.model.UserRepository;
import com.backend.observerr.auth.service.JwtService;
import com.backend.observerr.exam.model.Exam;
import com.backend.observerr.exam.model.ExamStatus;
import com.backend.observerr.exam.repository.ExamRepository;
import com.backend.observerr.integrity.model.ExamSession;
import com.backend.observerr.integrity.model.ExamSessionStatus;
import com.backend.observerr.integrity.repository.ExamSessionRepository;
import com.backend.observerr.lecturer.analytics.model.LecturerAnalyticsBehavior;
import com.backend.observerr.lecturer.analytics.model.LecturerAnalyticsOverview;
import com.backend.observerr.lecturer.analytics.model.LecturerAnalyticsTrendPoint;
import com.backend.observerr.lecturer.analytics.repository.LecturerAnalyticsOverviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LecturerDashboardIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private ExamSessionRepository examSessionRepository;

    @Autowired
    private LecturerAnalyticsOverviewRepository overviewRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private String lecturerToken;
    private User lecturer;
    private User student;
    private Exam liveExam;
    private UUID sessionId;

    @BeforeEach
    void setUp() {
        lecturer = userRepository.save(User.builder()
                .institutionalId("LEC-DASH-1")
                .email("dash-lecturer@test.com")
                .firstName("Dash")
                .lastName("Lecturer")
                .password(passwordEncoder.encode("password"))
                .role(Role.LECTURER)
                .build());

        student = userRepository.save(User.builder()
                .institutionalId("STU-DASH-1")
                .email("dash-student@test.com")
                .firstName("High")
                .lastName("Risk")
                .password(passwordEncoder.encode("password"))
                .role(Role.STUDENT)
                .build());

        Instant start = Instant.now().minus(30, ChronoUnit.MINUTES);
        liveExam = examRepository.save(Exam.builder()
                .title("Live Midterm")
                .lecturerId(lecturer.getId())
                .courseCode("CS204")
                .courseName("Data Structures")
                .durationMinutes(120)
                .startTime(start)
                .endTime(start.plus(120, ChronoUnit.MINUTES))
                .status(ExamStatus.SCHEDULED)
                .published(true)
                .enrolledCount(1)
                .webcamMonitoring(true)
                .tabSwitchTracking(true)
                .blockCopyPaste(true)
                .build());

        sessionId = UUID.randomUUID();
        examSessionRepository.save(ExamSession.builder()
                .id(sessionId)
                .examId(liveExam.getId())
                .studentId(student.getId())
                .startedAt(Instant.now().minus(20, ChronoUnit.MINUTES))
                .startingScore((short) 100)
                .totalDeductions(45)
                .totalEvents(2)
                .requiresReview(true)
                .status(ExamSessionStatus.IN_PROGRESS)
                .build());

        LecturerAnalyticsOverview overview = LecturerAnalyticsOverview.builder()
                .lecturerId(lecturer.getId())
                .period("7D")
                .totalExamsMonitored(10)
                .examsChangePercent(new BigDecimal("5.00"))
                .examsChangeDirection("UP")
                .examsChangeLabel("from last week")
                .totalFlaggedEvents(20)
                .flagsChangePercent(new BigDecimal("2.00"))
                .flagsChangeDirection("UP")
                .flagsChangeLabel("from last week")
                .avgIntegrityScore(new BigDecimal("90.00"))
                .integrityChangePercent(new BigDecimal("1.00"))
                .integrityChangeDirection("UP")
                .integrityChangeLabel("vs last week")
                .mostCommonFlagLabel("Tab Switch")
                .mostCommonFlagSharePercent(40)
                .mostCommonFlagIcon("tab")
                .trendGranularity("DAY")
                .trendSubtitle("Daily flagged events")
                .build();
        overview.getTrendPoints().add(LecturerAnalyticsTrendPoint.builder()
                .overview(overview)
                .label("Mon")
                .sortOrder(1)
                .monitoredSessions(10)
                .flaggedEvents(40)
                .alert(false)
                .build());
        overview.getBehaviors().add(LecturerAnalyticsBehavior.builder()
                .overview(overview)
                .behaviorCode("TAB_BLUR")
                .label("Tab Switch")
                .eventCount(64)
                .icon("tab")
                .tone("error")
                .sortOrder(1)
                .build());
        overviewRepository.save(overview);

        lecturerToken = jwtService.generateAccessToken(lecturer);
    }

    @Test
    void returnsDashboardForLecturer() throws Exception {
        mockMvc.perform(get("/api/lecturer/dashboard")
                        .header("Authorization", "Bearer " + lecturerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liveExam.examId").value(liveExam.getId()))
                .andExpect(jsonPath("$.liveExam.status").value("LIVE"))
                .andExpect(jsonPath("$.needsReview", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.needsReview[0].latestSessionId").value(sessionId.toString()))
                .andExpect(jsonPath("$.examTabs.live", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.integrityTrend.points", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.topFlaggedBehaviors[0].behaviorCode").value("TAB_BLUR"));
    }

    @Test
    void returnsLiveSessionsForOwnedExam() throws Exception {
        mockMvc.perform(get("/api/lecturer/exams/" + liveExam.getId() + "/live-sessions")
                        .header("Authorization", "Bearer " + lecturerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.examId").value(liveExam.getId()))
                .andExpect(jsonPath("$.students", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.students[0].latestSessionId").value(sessionId.toString()));
    }
}
