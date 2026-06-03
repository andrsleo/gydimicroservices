package com.affiliate.rentals.gydi.social.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

public final class Save {

    private final Long id;
    private final Long userId;
    private final Long contentPostId;
    private final LocalDateTime createdAt;

    private Save(Long id, Long userId, Long contentPostId, LocalDateTime createdAt) {
        this.id = id;
        this.userId = Objects.requireNonNull(userId, "userId requerido");
        this.contentPostId = Objects.requireNonNull(contentPostId, "contentPostId requerido");
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public static Save create(Long userId, Long contentPostId) {
        return new Save(null, userId, contentPostId, LocalDateTime.now());
    }

    public static Save reconstitute(Long id, Long userId, Long contentPostId, LocalDateTime createdAt) {
        return new Save(id, userId, contentPostId, createdAt);
    }

    public Long id() { return id; }
    public Long userId() { return userId; }
    public Long contentPostId() { return contentPostId; }
    public LocalDateTime createdAt() { return createdAt; }
}
