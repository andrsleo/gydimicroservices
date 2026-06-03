package com.affiliate.rentals.gydi.notifications.application.usecase;

import com.affiliate.rentals.gydi.notifications.application.dto.NotificationDto;
import com.affiliate.rentals.gydi.notifications.domain.model.Notification;
import com.affiliate.rentals.gydi.notifications.domain.ports.NotificationRepositoryPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetNotificationsUseCase {

    private final NotificationRepositoryPort repository;

    public GetNotificationsUseCase(NotificationRepositoryPort repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Page<NotificationDto> execute(Long recipientId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return repository.findByRecipientId(recipientId, pageable)
                .map(this::toDto);
    }

    private NotificationDto toDto(Notification n) {
        return new NotificationDto(
                n.getId(), n.getRecipientId(), n.getType(),
                n.getTitle(), n.getBody(), n.getEntityId(), n.getEntityType(),
                n.isRead(), n.getCreatedAt(), n.getReadAt());
    }
}
