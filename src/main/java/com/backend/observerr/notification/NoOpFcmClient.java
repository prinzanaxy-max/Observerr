package com.backend.observerr.notification;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnExpression("'${firebase.service-account-json:}'.trim().isEmpty()")
public class NoOpFcmClient implements FcmClient {

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public void send(Message message) throws FirebaseMessagingException {
        log.warn("FCM disabled — skipping single notification send");
    }

    @Override
    public BatchResponse sendEachForMulticast(MulticastMessage message) throws FirebaseMessagingException {
        log.warn("FCM disabled — skipping multicast notification send");
        return null;
    }
}
