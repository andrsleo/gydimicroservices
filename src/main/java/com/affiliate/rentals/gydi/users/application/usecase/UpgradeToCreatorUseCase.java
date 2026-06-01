package com.affiliate.rentals.gydi.users.application.usecase;

import com.affiliate.rentals.gydi.users.domain.model.CreatorProfile;
import com.affiliate.rentals.gydi.users.domain.ports.CreatorProfileRepositoryPort;
import com.affiliate.rentals.gydi.users.domain.ports.UserRepositoryPort;
import com.affiliate.rentals.gydi.users.domain.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpgradeToCreatorUseCase {

    private final UserRepositoryPort userRepository;
    private final CreatorProfileRepositoryPort creatorProfileRepository;

    @Transactional
    public void execute(Long userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado: " + userId));

        if (user.isVerifiedCreator()) return;

        userRepository.save(user.withVerifiedCreator(true));

        if (creatorProfileRepository.findByUserId(userId).isEmpty()) {
            creatorProfileRepository.save(CreatorProfile.create(userId));
        }

        log.info("Usuario habilitado como creator: userId={}", userId);
    }
}
