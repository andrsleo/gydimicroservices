package com.affiliate.rentals.gydi.notifications.infrastructure.out.persistence.entity;

import com.affiliate.rentals.gydi.notifications.domain.model.NotificationType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications", schema = "notifications")
public class NotificationJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private NotificationType type;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "body", length = 255)
    private String body;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "entity_type", length = 30)
    private String entityType;

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    // ─── Getters / Setters ───────────────────────────────────────────────────
    public Long getId()                         { return id; }
    public void setId(Long id)                  { this.id = id; }
    public Long getRecipientId()                { return recipientId; }
    public void setRecipientId(Long v)          { this.recipientId = v; }
    public NotificationType getType()           { return type; }
    public void setType(NotificationType v)     { this.type = v; }
    public String getTitle()                    { return title; }
    public void setTitle(String v)              { this.title = v; }
    public String getBody()                     { return body; }
    public void setBody(String v)               { this.body = v; }
    public Long getEntityId()                   { return entityId; }
    public void setEntityId(Long v)             { this.entityId = v; }
    public String getEntityType()               { return entityType; }
    public void setEntityType(String v)         { this.entityType = v; }
    public boolean isRead()                     { return isRead; }
    public void setRead(boolean v)              { this.isRead = v; }
    public LocalDateTime getCreatedAt()         { return createdAt; }
    public void setCreatedAt(LocalDateTime v)   { this.createdAt = v; }
    public LocalDateTime getReadAt()            { return readAt; }
    public void setReadAt(LocalDateTime v)      { this.readAt = v; }
}
