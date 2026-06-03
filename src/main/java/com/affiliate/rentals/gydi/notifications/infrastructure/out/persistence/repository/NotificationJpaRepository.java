package com.affiliate.rentals.gydi.notifications.infrastructure.out.persistence.repository;

import com.affiliate.rentals.gydi.notifications.infrastructure.out.persistence.entity.NotificationJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationJpaRepository extends JpaRepository<NotificationJpaEntity, Long> {

    Page<NotificationJpaEntity> findByRecipientId(Long recipientId, Pageable pageable);

    long countByRecipientIdAndIsRead(Long recipientId, boolean isRead);

    @Modifying
    @Query("UPDATE NotificationJpaEntity n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP " +
           "WHERE n.recipientId = :recipientId AND n.isRead = false")
    void markAllReadByRecipientId(@Param("recipientId") Long recipientId);
}
