package com.affiliate.rentals.gydi.notifications.infrastructure.out.persistence.adapter;

import com.affiliate.rentals.gydi.notifications.domain.model.Notification;
import com.affiliate.rentals.gydi.notifications.domain.ports.NotificationRepositoryPort;
import com.affiliate.rentals.gydi.notifications.infrastructure.out.persistence.entity.NotificationJpaEntity;
import com.affiliate.rentals.gydi.notifications.infrastructure.out.persistence.mapper.NotificationEntityMapper;
import com.affiliate.rentals.gydi.notifications.infrastructure.out.persistence.repository.NotificationJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class NotificationRepositoryAdapter implements NotificationRepositoryPort {

    private final NotificationJpaRepository jpaRepository;
    private final NotificationEntityMapper mapper;

    public NotificationRepositoryAdapter(NotificationJpaRepository jpaRepository,
                                         NotificationEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Notification save(Notification notification) {
        NotificationJpaEntity entity = mapper.toEntity(notification);
        NotificationJpaEntity saved = jpaRepository.save(entity);
        notification.setId(saved.getId());
        return notification;
    }

    @Override
    public Optional<Notification> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Page<Notification> findByRecipientId(Long recipientId, Pageable pageable) {
        return jpaRepository.findByRecipientId(recipientId, pageable).map(mapper::toDomain);
    }

    @Override
    public long countUnreadByRecipientId(Long recipientId) {
        return jpaRepository.countByRecipientIdAndIsRead(recipientId, false);
    }

    @Override
    public void markAllReadByRecipientId(Long recipientId) {
        jpaRepository.markAllReadByRecipientId(recipientId);
    }
}
