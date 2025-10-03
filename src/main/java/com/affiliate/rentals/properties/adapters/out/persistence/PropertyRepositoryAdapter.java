package com.affiliate.rentals.properties.adapters.out.persistence;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.affiliate.rentals.properties.adapters.out.persistence.entity.PropertyEntity;
import com.affiliate.rentals.properties.domain.model.Property;
import com.affiliate.rentals.properties.domain.port.PropertyRepositoryPort;

@Repository
@Transactional(readOnly = true)
public class PropertyRepositoryAdapter implements PropertyRepositoryPort {

    private final SpringDataPropertyRepository repo;

    public PropertyRepositoryAdapter(SpringDataPropertyRepository repo) {
        this.repo = repo;
    }

    @Override
    public Page<Property> findAll(Pageable pageable) {
        return repo.findAll(pageable).map(this::toDomain);
    }

    @Override
    public java.util.Optional<Property> findById(java.util.UUID id) {
        return repo.findById(id).map(this::toDomain);
    }

    private Property toDomain(PropertyEntity e) {
        return new Property(
            e.getId(),
            e.getOwnerId(),
            e.getTitle(),
            e.getDescription(),
            e.getLocation(),
            e.getPricePerNight(),
            e.getCurrency(),
            e.getBedrooms() == null ? 0 : e.getBedrooms(),
            e.getBathrooms() == null ? 0 : e.getBathrooms(),
            e.getBeds() == null ? 0 : e.getBeds(),
            e.getCapacity() == null ? 1 : e.getCapacity(),
            e.getCreatedAt()
        );
    }
}
