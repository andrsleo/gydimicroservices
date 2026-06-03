package com.affiliate.rentals.gydi.users.domain.ports;

import com.affiliate.rentals.gydi.users.domain.model.CreatorProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface CreatorProfileRepositoryPort {
    CreatorProfile save(CreatorProfile profile);
    Optional<CreatorProfile> findByUserId(Long userId);
    Page<CreatorProfile> findAll(Pageable pageable);
    List<CreatorProfile> findTop(int limit);
}
