package com.affiliate.rentals.properties.application.usecase.impl;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.affiliate.rentals.properties.application.dto.PropertyDto;
import com.affiliate.rentals.properties.application.usecase.GetAllPropertiesUseCase;
import com.affiliate.rentals.properties.domain.port.PropertyRepositoryPort;

@Service
@Transactional(readOnly = true)
public class GetAllPropertiesUseCaseImpl implements GetAllPropertiesUseCase {

    private final PropertyRepositoryPort repository;

    public GetAllPropertiesUseCaseImpl(PropertyRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    public Page<PropertyDto> execute(Pageable pageable) {
        return repository.findAll(pageable)
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
                ));
    }
}
