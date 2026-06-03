package com.affiliate.rentals.gydi.social.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

public final class Share {

    private final Long id;
    private final Long contentPostId;
    private final Long userId;
    private final SharePlatform platform;
    private final LocalDateTime createdAt;

    private Share(Long id, Long contentPostId, Long userId, SharePlatform platform, LocalDateTime createdAt) {
        this.id = id;
        this.contentPostId = Objects.requireNonNull(contentPostId, "contentPostId requerido");
        this.platform = Objects.requireNonNull(platform, "platform requerido");
        this.userId = userId;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public static Share create(Long contentPostId, Long userId, SharePlatform platform) {
        return new Share(null, contentPostId, userId, platform, LocalDateTime.now());
    }

    public static Share reconstitute(Long id, Long contentPostId, Long userId, SharePlatform platform, LocalDateTime createdAt) {
        return new Share(id, contentPostId, userId, platform, createdAt);
    }

    public Long id() { return id; }
    public Long contentPostId() { return contentPostId; }
    public Long userId() { return userId; }
    public SharePlatform platform() { return platform; }
    public LocalDateTime createdAt() { return createdAt; }
}
