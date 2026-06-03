package com.affiliate.rentals.gydi.notifications.application.usecase;

import com.affiliate.rentals.gydi.notifications.domain.model.Notification;
import com.affiliate.rentals.gydi.notifications.domain.model.NotificationType;
import com.affiliate.rentals.gydi.notifications.domain.ports.NotificationRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateNotificationUseCase {

    private final NotificationRepositoryPort repository;

    public CreateNotificationUseCase(NotificationRepositoryPort repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void execute(Long recipientId, NotificationType type,
                        String title, String body,
                        Long entityId, String entityType) {
        Notification notification = Notification.create(
                recipientId, type, title, body, entityId, entityType);
        repository.save(notification);
    }
}
