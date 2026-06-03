package com.affiliate.rentals.gydi.social.infrastructure.out.persistence.adapter;

import com.affiliate.rentals.gydi.social.application.port.ShareRepositoryPort;
import com.affiliate.rentals.gydi.social.domain.model.Share;
import com.affiliate.rentals.gydi.social.domain.model.SharePlatform;
import com.affiliate.rentals.gydi.social.infrastructure.out.persistence.entity.ShareEntity;
import com.affiliate.rentals.gydi.social.infrastructure.out.persistence.repository.ShareJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ShareRepositoryAdapter implements ShareRepositoryPort {

    private final ShareJpaRepository jpaRepository;

    @Override
    public Share save(Share share) {
        ShareEntity entity = new ShareEntity(null, share.contentPostId(), share.userId(),
                share.platform().name(), share.createdAt());
        ShareEntity saved = jpaRepository.save(entity);
        return Share.reconstitute(saved.getId(), saved.getContentPostId(), saved.getUserId(),
                SharePlatform.valueOf(saved.getPlatform()), saved.getCreatedAt());
    }
}
