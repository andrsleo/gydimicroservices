package com.affiliate.rentals.gydi.notifications.application.dto;

import com.affiliate.rentals.gydi.notifications.domain.model.NotificationType;
import java.time.LocalDateTime;

public record NotificationDto(
    Long id,
    Long recipientId,
    NotificationType type,
    String title,
    String body,
    Long entityId,
    String entityType,
    boolean isRead,
    LocalDateTime createdAt,
    LocalDateTime readAt
) {}
