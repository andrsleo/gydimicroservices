package com.affiliate.rentals.gydi.content.application.usecase;

import com.affiliate.rentals.gydi.content.application.dto.ContentPostDto;
import com.affiliate.rentals.gydi.content.application.mapper.ContentDtoMapper;
import com.affiliate.rentals.gydi.content.application.port.ContentPostRepositoryPort;
import com.affiliate.rentals.gydi.content.domain.event.ContentPublishedEvent;
import com.affiliate.rentals.gydi.content.domain.exception.ContentAccessDeniedException;
import com.affiliate.rentals.gydi.content.domain.exception.ContentNotFoundException;
import com.affiliate.rentals.gydi.content.domain.model.ContentPost;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for publishing a draft content post.
 *
 * @author GYDI Development Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PublishContentPostUseCase {

    private final ContentPostRepositoryPort contentPostRepository;
    private final ContentDtoMapper mapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ContentPostDto execute(Long contentPostId, Long requestingUserId) {
        ContentPost post = contentPostRepository.findById(contentPostId)
                .orElseThrow(() -> ContentNotFoundException.withId(contentPostId));

        if (!post.creatorId().equals(requestingUserId)) {
            throw new ContentAccessDeniedException("No tienes permiso para publicar este contenido");
        }

        post.publish();
        ContentPost saved = contentPostRepository.save(post);
        eventPublisher.publishEvent(new ContentPublishedEvent(saved.id(), saved.creatorId()));

        log.info("Contenido publicado: postId={}, creatorId={}", saved.id(), saved.creatorId());
        return mapper.toDto(saved);
    }
}
