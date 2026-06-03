package com.affiliate.rentals.gydi.content.application.usecase;

import com.affiliate.rentals.gydi.content.application.dto.ContentPostDto;
import com.affiliate.rentals.gydi.content.application.mapper.ContentDtoMapper;
import com.affiliate.rentals.gydi.content.application.port.ContentMediaRepositoryPort;
import com.affiliate.rentals.gydi.content.application.port.ContentPostRepositoryPort;
import com.affiliate.rentals.gydi.content.domain.exception.ContentNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetContentByIdUseCase {

    private final ContentPostRepositoryPort contentPostRepository;
    private final ContentMediaRepositoryPort contentMediaRepository;
    private final ContentDtoMapper mapper;

    public ContentPostDto execute(Long id) {
        var post = contentPostRepository.findById(id)
                .orElseThrow(() -> new ContentNotFoundException("Contenido no encontrado: " + id));
        var media = contentMediaRepository.findByContentPostId(id);
        return mapper.toDto(post, media);
    }
}
