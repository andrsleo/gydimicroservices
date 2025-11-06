package com.affiliate.rentals.gydi.properties.application.usecase;

import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.ports.in.PublishPropertyUseCase;
import com.affiliate.rentals.gydi.properties.domain.ports.out.PropertyRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PublishPropertyUseCaseImpl implements PublishPropertyUseCase {

    private final PropertyRepositoryPort propertyRepository;

    public PublishPropertyUseCaseImpl(PropertyRepositoryPort propertyRepository) {
        this.propertyRepository = propertyRepository;
    }

    @Override
    public Property publishProperty(PublishPropertyCommand command) {
        Property property = propertyRepository.findById(command.propertyId())
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));

        if (!property.isOwnedBy(command.requestingUserId())) {
            throw new SecurityException("User is not authorized to publish this property");
        }

        property.publish();

        return propertyRepository.save(property);
    }
}
