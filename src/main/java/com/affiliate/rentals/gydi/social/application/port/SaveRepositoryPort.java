package com.affiliate.rentals.gydi.social.application.port;

import com.affiliate.rentals.gydi.social.domain.model.Save;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface SaveRepositoryPort {
    Save save(Save save);
    Optional<Save> findByUserIdAndContentPostId(Long userId, Long contentPostId);
    void deleteByUserIdAndContentPostId(Long userId, Long contentPostId);
    boolean existsByUserIdAndContentPostId(Long userId, Long contentPostId);
    Page<Save> findByUserId(Long userId, Pageable pageable);
}
