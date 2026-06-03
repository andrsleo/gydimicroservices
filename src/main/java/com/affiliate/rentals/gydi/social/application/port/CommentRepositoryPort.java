package com.affiliate.rentals.gydi.social.application.port;

import com.affiliate.rentals.gydi.social.domain.model.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface CommentRepositoryPort {
    Comment save(Comment comment);
    Optional<Comment> findById(Long id);
    Page<Comment> findByContentPostId(Long contentPostId, Pageable pageable);
    void deleteById(Long id);
}
