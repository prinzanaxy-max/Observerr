package com.backend.observerr.notification;

import com.backend.observerr.notification.model.DeviceToken;

public interface WebPushClient {

    boolean isEnabled();

    /**
     * @return HTTP status from the push service, or -1 when delivery was skipped
     */
    int send(DeviceToken subscription, String payloadJson) throws Exception;
}
