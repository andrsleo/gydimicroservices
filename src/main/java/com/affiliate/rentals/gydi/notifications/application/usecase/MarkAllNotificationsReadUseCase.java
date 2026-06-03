package com.affiliate.rentals.gydi.notifications.application.usecase;

import com.affiliate.rentals.gydi.notifications.domain.ports.NotificationRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarkAllNotificationsReadUseCase {

    private final NotificationRepositoryPort repository;

    public MarkAllNotificationsReadUseCase(NotificationRepositoryPort repository) {
        this.repository = repository;
    }

    @Transactional
    public void execute(Long recipientId) {
        repository.markAllReadByRecipientId(recipientId);
    }
}
