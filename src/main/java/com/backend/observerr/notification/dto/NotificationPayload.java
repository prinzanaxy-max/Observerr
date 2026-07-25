package com.backend.observerr.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
public class NotificationPayload {

    private final String title;
    private final String body;
    private final Map<String, String> data;
    private final String webPushLink;
}
