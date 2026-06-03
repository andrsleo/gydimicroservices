package com.affiliate.rentals.gydi.notifications.application.usecase;

import com.affiliate.rentals.gydi.notifications.domain.model.Notification;
import com.affiliate.rentals.gydi.notifications.domain.ports.NotificationRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarkNotificationReadUseCase {

    private final NotificationRepositoryPort repository;

    public MarkNotificationReadUseCase(NotificationRepositoryPort repository) {
        this.repository = repository;
    }

    @Transactional
    public void execute(Long notificationId, Long requestingUserId) {
        Notification notification = repository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));

        if (!notification.getRecipientId().equals(requestingUserId)) {
            throw new SecurityException("Access denied to notification " + notificationId);
        }

        notification.markRead();
        repository.save(notification);
    }
}
