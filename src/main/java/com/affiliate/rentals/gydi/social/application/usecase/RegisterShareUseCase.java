package com.affiliate.rentals.gydi.social.application.usecase;

import com.affiliate.rentals.gydi.content.application.port.ContentPostRepositoryPort;
import com.affiliate.rentals.gydi.social.application.port.ShareRepositoryPort;
import com.affiliate.rentals.gydi.social.domain.model.Share;
import com.affiliate.rentals.gydi.social.domain.model.SharePlatform;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegisterShareUseCase {

    private final ShareRepositoryPort shareRepository;
    private final ContentPostRepositoryPort contentPostRepository;

    @Transactional
    public void execute(Long contentPostId, Long userId, SharePlatform platform) {
        shareRepository.save(Share.create(contentPostId, userId, platform));

        contentPostRepository.findById(contentPostId).ifPresent(post -> {
            post.incrementShareCount();
            contentPostRepository.save(post);
        });

        log.info("Share registrado: contentPostId={}, platform={}", contentPostId, platform);
    }
}
