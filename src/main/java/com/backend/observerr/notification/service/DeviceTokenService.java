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
        String endpoint = request.getEndpoint().trim();
        String p256dh = request.getKeys().getP256dh().trim();
        String auth = request.getKeys().getAuth().trim();

        deviceTokenRepository.findByEndpoint(endpoint)
                .ifPresentOrElse(
                        existing -> {
                            existing.setUserId(user.getId());
                            existing.setP256dh(p256dh);
                            existing.setAuth(auth);
                            deviceTokenRepository.save(existing);
                            log.info("Updated Web Push subscription userId={} tokenId={}",
                                    user.getId(), existing.getId());
                        },
                        () -> {
                            DeviceToken created = DeviceToken.builder()
                                    .userId(user.getId())
                                    .endpoint(endpoint)
                                    .p256dh(p256dh)
                                    .auth(auth)
                                    .build();
                            deviceTokenRepository.save(created);
                            log.info("Registered Web Push subscription userId={} tokenId={}",
                                    user.getId(), created.getId());
                        }
                );
    }

    @Transactional
    public void unregisterToken(User user, RegisterDeviceTokenRequest request) {
        deviceTokenRepository.findByEndpoint(request.getEndpoint().trim())
                .filter(existing -> existing.getUserId().equals(user.getId()))
                .ifPresent(deviceTokenRepository::delete);
    }
}
