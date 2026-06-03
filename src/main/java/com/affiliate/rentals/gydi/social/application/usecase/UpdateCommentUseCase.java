package com.affiliate.rentals.gydi.social.application.usecase;

import com.affiliate.rentals.gydi.social.application.dto.CommentDto;
import com.affiliate.rentals.gydi.social.application.dto.UpdateCommentRequest;
import com.affiliate.rentals.gydi.social.application.port.CommentRepositoryPort;
import com.affiliate.rentals.gydi.social.domain.model.Comment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateCommentUseCase {

    private final CommentRepositoryPort commentRepository;

    @Transactional
    public CommentDto execute(Long commentId, Long userId, UpdateCommentRequest request) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comentario no encontrado"));

        if (!comment.userId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo puedes editar tus propios comentarios");
        }

        comment.edit(request.body());
        Comment updated = commentRepository.save(comment);
        log.info("Comment updated: id={}, userId={}", commentId, userId);
        return AddCommentUseCase.toDto(updated);
    }
}
