package com.affiliate.rentals.gydi.content.application.usecase;

import com.affiliate.rentals.gydi.content.application.dto.ContentPostDto;
import com.affiliate.rentals.gydi.content.application.dto.UpdateContentPostRequest;
import com.affiliate.rentals.gydi.content.application.mapper.ContentDtoMapper;
import com.affiliate.rentals.gydi.content.application.port.ContentPostRepositoryPort;
import com.affiliate.rentals.gydi.content.domain.exception.ContentAccessDeniedException;
import com.affiliate.rentals.gydi.content.domain.exception.ContentNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateContentPostUseCase {

    private final ContentPostRepositoryPort contentPostRepository;
    private final ContentDtoMapper mapper;

    @Transactional
    public ContentPostDto execute(Long contentId, Long requesterId, UpdateContentPostRequest request) {
        var post = contentPostRepository.findById(contentId)
                .orElseThrow(() -> new ContentNotFoundException("Contenido no encontrado: " + contentId));

        if (!post.creatorId().equals(requesterId)) {
            throw new ContentAccessDeniedException("No tienes permiso para editar este contenido");
        }

        String sanitizedCaption = request.caption() != null
                ? HtmlUtils.htmlEscape(request.caption())
                : null;

        post.update(sanitizedCaption, request.propertyId());
        ContentPostDto dto = mapper.toDto(contentPostRepository.save(post));
        log.info("Contenido actualizado: postId={}", contentId);
        return dto;
    }
}
