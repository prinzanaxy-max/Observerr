package com.backend.observerr.notification.dto;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationItemDto {
    private Long id;
    private String category;
    private String title;
    private String message;
    private boolean read;
    private String createdAt;
    private String deepLink;
    private String deduplicationKey;
}
