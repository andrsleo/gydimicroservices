package com.affiliate.rentals.gydi.social.application.usecase;

import com.affiliate.rentals.gydi.content.application.port.ContentPostRepositoryPort;
import com.affiliate.rentals.gydi.social.application.dto.AddCommentRequest;
import com.affiliate.rentals.gydi.social.application.dto.CommentDto;
import com.affiliate.rentals.gydi.social.application.port.CommentRepositoryPort;
import com.affiliate.rentals.gydi.social.domain.model.Comment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AddCommentUseCase {

    private final CommentRepositoryPort commentRepository;
    private final ContentPostRepositoryPort contentPostRepository;

    @Transactional
    public CommentDto execute(Long contentPostId, Long userId, AddCommentRequest request) {
        Comment comment = Comment.create(contentPostId, userId, request.body(), request.parentCommentId());
        Comment saved = commentRepository.save(comment);

        contentPostRepository.findById(contentPostId).ifPresent(post -> {
            post.incrementCommentCount();
            contentPostRepository.save(post);
        });

        return toDto(saved);
    }

    public static CommentDto toDto(Comment comment) {
        return new CommentDto(comment.id(), comment.contentPostId(), comment.userId(),
                comment.parentCommentId(), comment.body(), comment.status(),
                comment.createdAt(), comment.updatedAt());
    }
}
