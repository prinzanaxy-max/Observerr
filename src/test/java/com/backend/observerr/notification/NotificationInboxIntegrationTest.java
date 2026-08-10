package com.backend.observerr.notification;

import com.backend.observerr.auth.model.Role;
import com.backend.observerr.auth.model.User;
import com.backend.observerr.auth.model.UserRepository;
import com.backend.observerr.auth.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class NotificationInboxIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;
    @Autowired NotificationInboxService inboxService;

    private User student;
    private String token;

    @BeforeEach
    void setUp() {
        student = userRepository.save(User.builder()
                .institutionalId("STU-NOTIFY-1")
                .email("notify-student@test.com")
                .password(passwordEncoder.encode("password"))
                .role(Role.STUDENT)
                .build());
        token = jwtService.generateAccessToken(student);
    }

    @Test
    void listsMarksAndDeletesOwnedNotifications() throws Exception {
        var notification = inboxService.create(
                student.getId(), "EXAM", "Exam Started", "Your exam is live",
                "/student/exams/1", "test-exam-start");

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Exam Started"))
                .andExpect(jsonPath("$.unreadCount").value(1));

        mockMvc.perform(patch("/api/notifications/{id}/read", notification.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/notifications/{id}", notification.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void preferencesSuppressDisabledCategoryAndRoundTrip() throws Exception {
        mockMvc.perform(put("/api/notifications/preferences")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"examEvents":false,"integrityAlerts":true,
                                 "resultUpdates":true,"systemUpdates":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.examEvents").value(false));

        org.assertj.core.api.Assertions.assertThat(inboxService.create(
                student.getId(), "EXAM", "Suppressed", "message", null, "suppressed"))
                .isNull();
    }
}
