package com.affiliate.rentals.gydi.notifications.infrastructure.out.persistence.mapper;

import com.affiliate.rentals.gydi.notifications.domain.model.Notification;
import com.affiliate.rentals.gydi.notifications.infrastructure.out.persistence.entity.NotificationJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class NotificationEntityMapper {

    public NotificationJpaEntity toEntity(Notification domain) {
        NotificationJpaEntity entity = new NotificationJpaEntity();
        entity.setId(domain.getId());
        entity.setRecipientId(domain.getRecipientId());
        entity.setType(domain.getType());
        entity.setTitle(domain.getTitle());
        entity.setBody(domain.getBody());
        entity.setEntityId(domain.getEntityId());
        entity.setEntityType(domain.getEntityType());
        entity.setRead(domain.isRead());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setReadAt(domain.getReadAt());
        return entity;
    }

    public Notification toDomain(NotificationJpaEntity entity) {
        return Notification.reconstitute(
                entity.getId(),
                entity.getRecipientId(),
                entity.getType(),
                entity.getTitle(),
                entity.getBody(),
                entity.getEntityId(),
                entity.getEntityType(),
                entity.isRead(),
                entity.getCreatedAt(),
                entity.getReadAt()
        );
    }
}
