package com.backend.observerr.notification;

import com.backend.observerr.auth.model.Role;
import com.backend.observerr.auth.model.User;
import com.backend.observerr.auth.model.UserRepository;
import com.backend.observerr.auth.service.JwtService;
import com.backend.observerr.notification.repository.DeviceTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DeviceTokenLifecycleIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private DeviceTokenRepository deviceTokenRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    @Test
    void subscriptionCanRegisterTransferAndOnlyCurrentOwnerCanUnregister() throws Exception {
        User first = student("STU-DEVICE-1", "device-1@test.com");
        User second = student("STU-DEVICE-2", "device-2@test.com");
        String firstJwt = jwtService.generateAccessToken(first);
        String secondJwt = jwtService.generateAccessToken(second);
        String body = """
                {
                  "endpoint": "https://push.example/shared-subscription",
                  "keys": { "p256dh": "p256dh-key", "auth": "auth-key" }
                }
                """;

        mockMvc.perform(post("/api/devices/token")
                        .header("Authorization", "Bearer " + firstJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
        assertThat(deviceTokenRepository.findByEndpoint("https://push.example/shared-subscription"))
                .get().extracting(token -> token.getUserId()).isEqualTo(first.getId());

        mockMvc.perform(post("/api/devices/token")
                        .header("Authorization", "Bearer " + secondJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
        assertThat(deviceTokenRepository.findByEndpoint("https://push.example/shared-subscription"))
                .get().extracting(token -> token.getUserId()).isEqualTo(second.getId());

        mockMvc.perform(delete("/api/devices/token")
                        .header("Authorization", "Bearer " + firstJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());
        assertThat(deviceTokenRepository.findByEndpoint("https://push.example/shared-subscription")).isPresent();

        mockMvc.perform(delete("/api/devices/token")
                        .header("Authorization", "Bearer " + secondJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());
        assertThat(deviceTokenRepository.findByEndpoint("https://push.example/shared-subscription")).isEmpty();
    }

    private User student(String institutionalId, String email) {
        return userRepository.save(User.builder()
                .institutionalId(institutionalId)
                .email(email)
                .password(passwordEncoder.encode("password"))
                .role(Role.STUDENT)
                .build());
    }
}
