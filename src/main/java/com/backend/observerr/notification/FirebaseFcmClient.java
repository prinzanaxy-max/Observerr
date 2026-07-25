package com.backend.observerr.notification;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(FirebaseMessaging.class)
@RequiredArgsConstructor
public class FirebaseFcmClient implements FcmClient {

    private final FirebaseMessaging firebaseMessaging;

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void send(Message message) throws FirebaseMessagingException {
        firebaseMessaging.send(message);
    }

    @Override
    public BatchResponse sendEachForMulticast(MulticastMessage message) throws FirebaseMessagingException {
        return firebaseMessaging.sendEachForMulticast(message);
    }
}
