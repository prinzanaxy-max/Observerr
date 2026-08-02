package com.backend.observerr.notification.dto;

import lombok.*;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPageDto {
    private List<NotificationItemDto> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private long unreadCount;
}
