package com.affiliate.rentals.gydi.properties.application.usecase;

import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.ports.in.DeletePropertyUseCase;
import com.affiliate.rentals.gydi.properties.domain.ports.out.PropertyRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DeletePropertyUseCaseImpl implements DeletePropertyUseCase {

    private final PropertyRepositoryPort propertyRepository;

    public DeletePropertyUseCaseImpl(PropertyRepositoryPort propertyRepository) {
        this.propertyRepository = propertyRepository;
    }

    @Override
    public void deleteProperty(DeletePropertyCommand command) {
        Property property = propertyRepository.findById(command.propertyId())
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));

        if (!property.isOwnedBy(command.requestingUserId())) {
            throw new SecurityException("User is not authorized to delete this property");
        }

        property.delete();
        propertyRepository.save(property);
    }
}
