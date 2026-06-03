package com.affiliate.rentals.gydi.content.application.usecase;

import com.affiliate.rentals.gydi.content.application.dto.ContentPostDto;
import com.affiliate.rentals.gydi.content.application.mapper.ContentDtoMapper;
import com.affiliate.rentals.gydi.content.application.port.ContentMediaRepositoryPort;
import com.affiliate.rentals.gydi.content.application.port.ContentPostRepositoryPort;
import com.affiliate.rentals.gydi.content.domain.model.ContentMedia;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetCreatorContentUseCase {

    private final ContentPostRepositoryPort contentPostRepository;
    private final ContentMediaRepositoryPort contentMediaRepository;
    private final ContentDtoMapper mapper;

    public Page<ContentPostDto> execute(Long creatorId, Pageable pageable) {
        return contentPostRepository.findByCreatorId(creatorId, pageable).map(post -> {
            List<ContentMedia> media = contentMediaRepository.findByContentPostId(post.id());
            return mapper.toDto(post, media);
        });
    }
}
