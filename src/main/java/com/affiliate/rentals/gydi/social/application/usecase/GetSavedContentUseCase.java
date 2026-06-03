package com.affiliate.rentals.gydi.social.application.usecase;

import com.affiliate.rentals.gydi.social.application.dto.SavedItemDto;
import com.affiliate.rentals.gydi.social.application.port.SaveRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetSavedContentUseCase {

    private final SaveRepositoryPort saveRepository;

    public Page<SavedItemDto> execute(Long userId, Pageable pageable) {
        return saveRepository.findByUserId(userId, pageable)
                .map(s -> new SavedItemDto(s.contentPostId(), s.createdAt()));
    }
}
