package com.interviewai.dto.response;

import com.interviewai.domain.Notification;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        String type,
        String title,
        String message,
        boolean read,
        Instant createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType().name(),
                n.getTitle(),
                n.getMessage(),
                n.isRead(),
                n.getCreatedAt());
    }
}
