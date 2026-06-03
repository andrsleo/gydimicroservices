package com.affiliate.rentals.gydi.social.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

public final class Comment {

    private final Long id;
    private final Long contentPostId;
    private final Long userId;
    private final Long parentCommentId;
    private String body;
    private CommentStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Comment(Long id, Long contentPostId, Long userId, Long parentCommentId,
                    String body, CommentStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.contentPostId = Objects.requireNonNull(contentPostId, "contentPostId requerido");
        this.userId = Objects.requireNonNull(userId, "userId requerido");
        this.parentCommentId = parentCommentId;
        this.body = validateBody(body);
        this.status = status != null ? status : CommentStatus.VISIBLE;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedAt = updatedAt != null ? updatedAt : LocalDateTime.now();
    }

    private static String validateBody(String body) {
        Objects.requireNonNull(body, "El comentario no puede estar vacío");
        String trimmed = body.trim();
        if (trimmed.length() < 3) throw new IllegalArgumentException("El comentario debe tener al menos 3 caracteres");
        if (trimmed.length() > 300) throw new IllegalArgumentException("El comentario no puede superar 300 caracteres");
        return trimmed;
    }

    public static Comment create(Long contentPostId, Long userId, String body, Long parentCommentId) {
        return new Comment(null, contentPostId, userId, parentCommentId, body, CommentStatus.VISIBLE,
                LocalDateTime.now(), LocalDateTime.now());
    }

    public static Comment reconstitute(Long id, Long contentPostId, Long userId, Long parentCommentId,
                                       String body, CommentStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Comment(id, contentPostId, userId, parentCommentId, body, status, createdAt, updatedAt);
    }

    public void edit(String newBody) {
        this.body = validateBody(newBody);
        this.updatedAt = LocalDateTime.now();
    }

    public void delete() {
        this.status = CommentStatus.DELETED;
        this.updatedAt = LocalDateTime.now();
    }

    public Long id() { return id; }
    public Long contentPostId() { return contentPostId; }
    public Long userId() { return userId; }
    public Long parentCommentId() { return parentCommentId; }
    public String body() { return body; }
    public CommentStatus status() { return status; }
    public LocalDateTime createdAt() { return createdAt; }
    public LocalDateTime updatedAt() { return updatedAt; }
}
