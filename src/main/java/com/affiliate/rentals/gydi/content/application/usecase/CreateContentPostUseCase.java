package com.affiliate.rentals.gydi.content.application.usecase;

import com.affiliate.rentals.gydi.content.application.dto.ContentPostDto;
import com.affiliate.rentals.gydi.content.application.dto.CreateContentPostRequest;
import com.affiliate.rentals.gydi.content.application.mapper.ContentDtoMapper;
import com.affiliate.rentals.gydi.content.application.port.ContentPostRepositoryPort;
import com.affiliate.rentals.gydi.content.domain.event.ContentCreatedEvent;
import com.affiliate.rentals.gydi.content.domain.model.ContentPost;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

/**
 * Use case for creating a new content post.
 *
 * <p>Sanitizes caption input using Spring's HtmlUtils.htmlEscape() to prevent XSS.</p>
 *
 * @author GYDI Development Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreateContentPostUseCase {

    private final ContentPostRepositoryPort contentPostRepository;
    private final ContentDtoMapper mapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ContentPostDto execute(Long creatorId, CreateContentPostRequest request) {
        String sanitizedCaption = request.caption() != null
                ? HtmlUtils.htmlEscape(request.caption())
                : null;

        ContentPost post = ContentPost.create(
                creatorId,
                sanitizedCaption,
                request.type(),
                request.propertyId()
        );

        ContentPost saved = contentPostRepository.save(post);
        eventPublisher.publishEvent(new ContentCreatedEvent(saved.id(), saved.creatorId(), saved.propertyId()));

        log.info("Contenido creado: postId={}, creatorId={}", saved.id(), saved.creatorId());
        return mapper.toDto(saved);
    }
}
