package com.affiliate.rentals.gydi.social.application.usecase;

import com.affiliate.rentals.gydi.social.application.dto.CommentDto;
import com.affiliate.rentals.gydi.social.application.port.CommentRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetCommentsUseCase {

    private final CommentRepositoryPort commentRepository;

    public Page<CommentDto> execute(Long contentPostId, Pageable pageable) {
        return commentRepository.findByContentPostId(contentPostId, pageable)
                .map(AddCommentUseCase::toDto);
    }
}
