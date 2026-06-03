package com.affiliate.rentals.gydi.notifications.domain.model;

import java.time.LocalDateTime;

/**
 * Notification aggregate root.
 * Pure domain model — no framework dependencies.
 */
public class Notification {

    private Long id;
    private final Long recipientId;
    private final NotificationType type;
    private final String title;
    private final String body;
    private final Long entityId;
    private final String entityType;
    private boolean isRead;
    private final LocalDateTime createdAt;
    private LocalDateTime readAt;

    // Private constructor — use factory methods
    private Notification(Long id, Long recipientId, NotificationType type,
                         String title, String body, Long entityId, String entityType,
                         boolean isRead, LocalDateTime createdAt, LocalDateTime readAt) {
        this.id = id;
        this.recipientId = recipientId;
        this.type = type;
        this.title = title;
        this.body = body;
        this.entityId = entityId;
        this.entityType = entityType;
        this.isRead = isRead;
        this.createdAt = createdAt;
        this.readAt = readAt;
    }

    /** Factory — create new notification (not yet persisted) */
    public static Notification create(Long recipientId, NotificationType type,
                                      String title, String body,
                                      Long entityId, String entityType) {
        return new Notification(null, recipientId, type, title, body,
                entityId, entityType, false, LocalDateTime.now(), null);
    }

    /** Factory — reconstitute from persistence */
    public static Notification reconstitute(Long id, Long recipientId, NotificationType type,
                                            String title, String body, Long entityId,
                                            String entityType, boolean isRead,
                                            LocalDateTime createdAt, LocalDateTime readAt) {
        return new Notification(id, recipientId, type, title, body,
                entityId, entityType, isRead, createdAt, readAt);
    }

    /** Mark as read — idempotent */
    public void markRead() {
        if (!isRead) {
            this.isRead = true;
            this.readAt = LocalDateTime.now();
        }
    }

    // ─── Getters ─────────────────────────────────────────────────────────────
    public Long getId()                  { return id; }
    public void setId(Long id)           { this.id = id; }
    public Long getRecipientId()         { return recipientId; }
    public NotificationType getType()    { return type; }
    public String getTitle()             { return title; }
    public String getBody()              { return body; }
    public Long getEntityId()            { return entityId; }
    public String getEntityType()        { return entityType; }
    public boolean isRead()              { return isRead; }
    public LocalDateTime getCreatedAt()  { return createdAt; }
    public LocalDateTime getReadAt()     { return readAt; }
}
