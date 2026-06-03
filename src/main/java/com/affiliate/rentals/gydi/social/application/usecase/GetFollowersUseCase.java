package com.affiliate.rentals.gydi.social.application.usecase;

import com.affiliate.rentals.gydi.social.application.dto.FollowUserDto;
import com.affiliate.rentals.gydi.social.application.port.FollowRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetFollowersUseCase {

    private final FollowRepositoryPort followRepository;

    public Page<FollowUserDto> execute(Long userId, Pageable pageable) {
        return followRepository.findByFollowingId(userId, pageable)
                .map(f -> new FollowUserDto(f.followerId(), f.createdAt()));
    }
}
