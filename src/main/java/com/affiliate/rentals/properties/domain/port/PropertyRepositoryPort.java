package com.affiliate.rentals.properties.domain.port;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.affiliate.rentals.properties.domain.model.Property;

public interface PropertyRepositoryPort {
    Page<Property> findAll(Pageable pageable);
    Optional<Property> findById(UUID id);
}
