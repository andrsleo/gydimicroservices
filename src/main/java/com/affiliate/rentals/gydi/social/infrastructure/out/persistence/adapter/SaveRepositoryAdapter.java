package com.affiliate.rentals.gydi.social.infrastructure.out.persistence.adapter;

import com.affiliate.rentals.gydi.social.application.port.SaveRepositoryPort;
import com.affiliate.rentals.gydi.social.domain.model.Save;
import com.affiliate.rentals.gydi.social.infrastructure.out.persistence.entity.SaveEntity;
import com.affiliate.rentals.gydi.social.infrastructure.out.persistence.repository.SaveJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SaveRepositoryAdapter implements SaveRepositoryPort {

    private final SaveJpaRepository jpaRepository;

    @Override
    public Save save(Save save) {
        SaveEntity entity = new SaveEntity(null, save.userId(), save.contentPostId(), save.createdAt());
        SaveEntity saved = jpaRepository.save(entity);
        return Save.reconstitute(saved.getId(), saved.getUserId(), saved.getContentPostId(), saved.getCreatedAt());
    }

    @Override
    public Optional<Save> findByUserIdAndContentPostId(Long userId, Long contentPostId) {
        return jpaRepository.findByUserIdAndContentPostId(userId, contentPostId)
                .map(e -> Save.reconstitute(e.getId(), e.getUserId(), e.getContentPostId(), e.getCreatedAt()));
    }

    @Override
    @Transactional
    public void deleteByUserIdAndContentPostId(Long userId, Long contentPostId) {
        jpaRepository.deleteByUserIdAndContentPostId(userId, contentPostId);
    }

    @Override
    public boolean existsByUserIdAndContentPostId(Long userId, Long contentPostId) {
        return jpaRepository.existsByUserIdAndContentPostId(userId, contentPostId);
    }

    @Override
    public Page<Save> findByUserId(Long userId, Pageable pageable) {
        return jpaRepository.findByUserId(userId, pageable)
                .map(e -> Save.reconstitute(e.getId(), e.getUserId(), e.getContentPostId(), e.getCreatedAt()));
    }
}
