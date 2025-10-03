package com.affiliate.rentals.properties.application.usecase.impl;


import java.util.UUID;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.affiliate.rentals.properties.application.dto.PropertyDto;
import com.affiliate.rentals.properties.application.usecase.GetPropertyByIdUseCase;
import com.affiliate.rentals.properties.common.exception.ResourceNotFoundException;
import com.affiliate.rentals.properties.domain.port.PropertyRepositoryPort;

@Service
@Transactional(readOnly = true)
public class GetPropertyByIdUseCaseImpl implements GetPropertyByIdUseCase {

    private final PropertyRepositoryPort repository;

    public GetPropertyByIdUseCaseImpl(PropertyRepositoryPort repository) {
        this.repository = repository;
    }

    // Caching para lecturas frecuentes
    @Override
    @Cacheable(value = "properties", key = "#id")
    public PropertyDto execute(UUID id) {
        return repository.findById(id)
            .map(p -> new PropertyDto(
                    p.id(),
                    p.ownerId(),
                    p.title(),
                    p.description(),
                    p.location(),
                    p.pricePerNight(),
                    p.currency(),
                    p.bedrooms(),
                    p.bathrooms(),
                    p.beds(),
                    p.capacity(),
                    p.createdAt()
            ))
            .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + id));
    }
}
