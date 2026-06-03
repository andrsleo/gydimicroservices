package com.affiliate.rentals.gydi.content.application.usecase;

import com.affiliate.rentals.gydi.content.application.dto.ContentPostDto;
import com.affiliate.rentals.gydi.content.application.mapper.ContentDtoMapper;
import com.affiliate.rentals.gydi.content.application.port.ContentPostRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetPropertyContentUseCase {

    private final ContentPostRepositoryPort contentPostRepository;
    private final ContentDtoMapper mapper;

    public Page<ContentPostDto> execute(Long propertyId, Pageable pageable) {
        return contentPostRepository.findByPropertyId(propertyId, pageable).map(mapper::toDto);
    }
}
