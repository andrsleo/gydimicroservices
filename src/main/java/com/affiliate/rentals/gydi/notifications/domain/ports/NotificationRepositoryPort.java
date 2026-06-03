package com.affiliate.rentals.gydi.notifications.domain.ports;

import com.affiliate.rentals.gydi.notifications.domain.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface NotificationRepositoryPort {
    Notification save(Notification notification);
    Optional<Notification> findById(Long id);
    Page<Notification> findByRecipientId(Long recipientId, Pageable pageable);
    long countUnreadByRecipientId(Long recipientId);
    void markAllReadByRecipientId(Long recipientId);
}
