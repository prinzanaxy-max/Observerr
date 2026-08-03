package com.backend.observerr.notification;

import com.backend.observerr.notification.model.DeviceToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnExpression(
        "'${webpush.vapid.public-key:}'.trim().isEmpty() || '${webpush.vapid.private-key:}'.trim().isEmpty()"
)
public class NoOpWebPushClient implements WebPushClient {

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public int send(DeviceToken subscription, String payloadJson) {
        log.debug("Web Push disabled — skipping endpoint={}", subscription.getEndpoint());
        return -1;
    }
}
