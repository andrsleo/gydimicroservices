package com.affiliate.rentals.gydi.social.application.dto;

import com.affiliate.rentals.gydi.social.domain.model.CommentStatus;

import java.time.LocalDateTime;

public record CommentDto(
        Long id,
        Long contentPostId,
        Long userId,
        Long parentCommentId,
        String body,
        CommentStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
