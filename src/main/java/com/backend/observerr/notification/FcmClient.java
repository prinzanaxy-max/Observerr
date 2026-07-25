package com.backend.observerr.notification;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;

public interface FcmClient {

    boolean isEnabled();

    void send(Message message) throws FirebaseMessagingException;

    BatchResponse sendEachForMulticast(MulticastMessage message) throws FirebaseMessagingException;
}
