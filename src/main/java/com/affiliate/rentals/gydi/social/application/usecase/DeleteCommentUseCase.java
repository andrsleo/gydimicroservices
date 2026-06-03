package com.affiliate.rentals.gydi.social.application.usecase;

import com.affiliate.rentals.gydi.content.application.port.ContentPostRepositoryPort;
import com.affiliate.rentals.gydi.social.application.port.CommentRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeleteCommentUseCase {

    private final CommentRepositoryPort commentRepository;
    private final ContentPostRepositoryPort contentPostRepository;

    @Transactional
    public void execute(Long commentId, Long userId, boolean isAdmin) {
        var comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comentario no encontrado: " + commentId));

        if (!isAdmin && !comment.userId().equals(userId)) {
            throw new IllegalStateException("No tienes permiso para eliminar este comentario");
        }

        comment.delete();
        commentRepository.save(comment);

        contentPostRepository.findById(comment.contentPostId()).ifPresent(post -> {
            post.decrementCommentCount();
            contentPostRepository.save(post);
        });
    }
}
