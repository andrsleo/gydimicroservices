package com.affiliate.rentals.gydi.notifications.application.usecase;

import com.affiliate.rentals.gydi.notifications.application.dto.UnreadCountDto;
import com.affiliate.rentals.gydi.notifications.domain.ports.NotificationRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetUnreadCountUseCase {

    private final NotificationRepositoryPort repository;

    public GetUnreadCountUseCase(NotificationRepositoryPort repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public UnreadCountDto execute(Long recipientId) {
        return new UnreadCountDto(repository.countUnreadByRecipientId(recipientId));
    }
}
