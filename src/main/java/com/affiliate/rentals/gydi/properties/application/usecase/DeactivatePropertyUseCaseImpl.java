package com.affiliate.rentals.gydi.properties.application.usecase;

import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.ports.in.DeactivatePropertyUseCase;
import com.affiliate.rentals.gydi.properties.domain.ports.out.PropertyRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DeactivatePropertyUseCaseImpl implements DeactivatePropertyUseCase {

    private final PropertyRepositoryPort propertyRepository;

    public DeactivatePropertyUseCaseImpl(PropertyRepositoryPort propertyRepository) {
        this.propertyRepository = propertyRepository;
    }

    @Override
    public Property deactivateProperty(DeactivatePropertyCommand command) {
        Property property = propertyRepository.findById(command.propertyId())
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));

        if (!property.isOwnedBy(command.requestingUserId())) {
            throw new SecurityException("User is not authorized to deactivate this property");
        }

        property.deactivate();

        return propertyRepository.save(property);
    }
}