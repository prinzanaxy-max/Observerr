package com.backend.observerr.notification.service;

import com.backend.observerr.auth.model.User;
import com.backend.observerr.notification.dto.RegisterDeviceTokenRequest;
import com.backend.observerr.notification.model.DeviceToken;
import com.backend.observerr.notification.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceTokenService {

    private final DeviceTokenRepository deviceTokenRepository;

    @Transactional
    public void registerToken(User user, RegisterDeviceTokenRequest request) {
        deviceTokenRepository.findByToken(request.getToken())
                .ifPresentOrElse(
                        existing -> {
                            existing.setUserId(user.getId());
                            deviceTokenRepository.save(existing);
                            log.info("Updated FCM token mapping userId={} tokenId={}", user.getId(), existing.getId());
                        },
                        () -> {
                            DeviceToken created = DeviceToken.builder()
                                    .userId(user.getId())
                                    .token(request.getToken())
                                    .build();
                            deviceTokenRepository.save(created);
                            log.info("Registered FCM token userId={} tokenId={}", user.getId(), created.getId());
                        }
                );
    }

    @Transactional
    public void unregisterToken(User user, RegisterDeviceTokenRequest request) {
        deviceTokenRepository.findByToken(request.getToken())
                .filter(existing -> existing.getUserId().equals(user.getId()))
                .ifPresent(deviceTokenRepository::delete);
    }
}
