package com.affiliate.rentals.gydi.social.infrastructure.out.persistence.adapter;

import com.affiliate.rentals.gydi.social.application.port.CommentRepositoryPort;
import com.affiliate.rentals.gydi.social.domain.model.Comment;
import com.affiliate.rentals.gydi.social.domain.model.CommentStatus;
import com.affiliate.rentals.gydi.social.infrastructure.out.persistence.entity.CommentEntity;
import com.affiliate.rentals.gydi.social.infrastructure.out.persistence.repository.CommentJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CommentRepositoryAdapter implements CommentRepositoryPort {

    private final CommentJpaRepository jpaRepository;

    @Override
    public Comment save(Comment comment) {
        CommentEntity entity = toEntity(comment);
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<Comment> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Page<Comment> findByContentPostId(Long contentPostId, Pageable pageable) {
        return jpaRepository.findByContentPostIdAndStatusNot(contentPostId, "DELETED", pageable)
                .map(this::toDomain);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    private CommentEntity toEntity(Comment c) {
        CommentEntity e = new CommentEntity();
        e.setId(c.id());
        e.setContentPostId(c.contentPostId());
        e.setUserId(c.userId());
        e.setParentCommentId(c.parentCommentId());
        e.setBody(c.body());
        e.setStatus(c.status().name());
        e.setCreatedAt(c.createdAt());
        e.setUpdatedAt(c.updatedAt());
        return e;
    }

    private Comment toDomain(CommentEntity e) {
        return Comment.reconstitute(e.getId(), e.getContentPostId(), e.getUserId(), e.getParentCommentId(),
                e.getBody(), CommentStatus.valueOf(e.getStatus()), e.getCreatedAt(), e.getUpdatedAt());
    }
}
