package com.backend.observerr.lecturer.analytics;

import com.backend.observerr.auth.model.Role;
import com.backend.observerr.auth.model.User;
import com.backend.observerr.auth.model.UserRepository;
import com.backend.observerr.auth.service.JwtService;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LecturerAnalyticsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LecturerAnalyticsOverviewRepository overviewRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private String lecturerToken;

    @BeforeEach
    void setUp() {
        User lecturer = userRepository.save(User.builder()
                .institutionalId("LEC-ANALYTICS-1")
                .email("lecturer-analytics@test.com")
                .firstName("Analytics")
                .lastName("Lecturer")
                .password(passwordEncoder.encode("password"))
                .role(Role.LECTURER)
                .build());

        LecturerAnalyticsOverview overview = LecturerAnalyticsOverview.builder()
                .lecturerId(lecturer.getId())
                .period("7D")
                .totalExamsMonitored(312)
                .examsChangePercent(new BigDecimal("8.00"))
                .examsChangeDirection("UP")
                .examsChangeLabel("from last week")
                .totalFlaggedEvents(89)
                .flagsChangePercent(new BigDecimal("3.00"))
                .flagsChangeDirection("UP")
                .flagsChangeLabel("from last week")
                .avgIntegrityScore(new BigDecimal("95.10"))
                .integrityChangePercent(new BigDecimal("0.40"))
                .integrityChangeDirection("UP")
                .integrityChangeLabel("vs last week")
                .mostCommonFlagLabel("Face Not Visible")
                .mostCommonFlagSharePercent(45)
                .mostCommonFlagIcon("visibility_off")
                .trendGranularity("DAY")
                .trendSubtitle("Daily flagged events vs monitored sessions")
                .build();

        overview.getTrendPoints().add(LecturerAnalyticsTrendPoint.builder()
                .overview(overview)
                .label("Thu")
                .sortOrder(4)
                .monitoredSessions(60)
                .flaggedEvents(28)
                .alert(true)
                .build());

        overview.getBehaviors().add(LecturerAnalyticsBehavior.builder()
                .overview(overview)
                .behaviorCode("FACE_NOT_VISIBLE")
                .label("Face Not Visible")
                .eventCount(43)
                .icon("visibility_off")
                .tone("error")
                .sortOrder(1)
                .build());

        overviewRepository.save(overview);
        lecturerToken = jwtService.generateAccessToken(lecturer);
    }

    @Test
    void returnsSeededAnalyticsOverview() throws Exception {
        mockMvc.perform(get("/api/lecturer/analytics/overview")
                        .param("period", "7D")
                        .header("Authorization", "Bearer " + lecturerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period").value("7D"))
                .andExpect(jsonPath("$.totalExamsMonitored.value").value(312))
                .andExpect(jsonPath("$.totalFlaggedEvents.value").value(89))
                .andExpect(jsonPath("$.avgIntegrityScore.value").value(95.10))
                .andExpect(jsonPath("$.mostCommonFlag.label").value("Face Not Visible"))
                .andExpect(jsonPath("$.trends.points[0].alert").value(true))
                .andExpect(jsonPath("$.topBehaviors[0].eventCount").value(43));
    }

    @Test
    void rejectsUnsupportedPeriod() throws Exception {
        mockMvc.perform(get("/api/lecturer/analytics/overview")
                        .param("period", "1Y")
                        .header("Authorization", "Bearer " + lecturerToken))
                .andExpect(status().isBadRequest());
    }
}
