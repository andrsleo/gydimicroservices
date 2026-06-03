package com.affiliate.rentals.gydi.content.application.usecase;

import com.affiliate.rentals.gydi.content.application.port.ContentPostRepositoryPort;
import com.affiliate.rentals.gydi.content.domain.event.ContentArchivedEvent;
import com.affiliate.rentals.gydi.content.domain.exception.ContentAccessDeniedException;
import com.affiliate.rentals.gydi.content.domain.exception.ContentNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiveContentPostUseCase {

    private final ContentPostRepositoryPort contentPostRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void execute(Long contentId, Long requesterId, boolean isAdmin) {
        var post = contentPostRepository.findById(contentId)
                .orElseThrow(() -> new ContentNotFoundException("Contenido no encontrado: " + contentId));

        if (!isAdmin && !post.creatorId().equals(requesterId)) {
            throw new ContentAccessDeniedException("No tienes permiso para archivar este contenido");
        }

        post.archive();
        contentPostRepository.save(post);
        eventPublisher.publishEvent(new ContentArchivedEvent(post.id(), post.creatorId()));
        log.info("Contenido archivado: postId={}", contentId);
    }
}
