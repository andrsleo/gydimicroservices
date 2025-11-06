package com.affiliate.rentals.gydi.properties.application.usecase;

import com.affiliate.rentals.gydi.properties.domain.model.*;
import com.affiliate.rentals.gydi.properties.domain.ports.in.UpdatePropertyUseCase;
import com.affiliate.rentals.gydi.properties.domain.ports.out.PropertyRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Currency;

@Service
@Transactional
public class UpdatePropertyUseCaseImpl implements UpdatePropertyUseCase {

    private final PropertyRepositoryPort propertyRepository;

    public UpdatePropertyUseCaseImpl(PropertyRepositoryPort propertyRepository) {
        this.propertyRepository = propertyRepository;
    }

    @Override
    public Property updateProperty(UpdatePropertyCommand command) {
        Property property = propertyRepository.findById(command.propertyId())
                .orElseThrow(() -> new IllegalArgumentException("Property not found: " + command.propertyId()));

        if (!property.isOwnedBy(command.requestingUserId())) {
            throw new SecurityException("User is not authorized to update this property");
        }

        if (command.title() != null || command.description() != null || 
            (command.priceAmount() != null && command.priceCurrency() != null)) {
            Money newPrice = command.priceAmount() != null && command.priceCurrency() != null
                ? Money.of(command.priceAmount(), command.priceCurrency())
                : null;
            property.updateDetails(command.title(), command.description(), newPrice);
        }

        if (command.country() != null && command.city() != null) {
            property.updateLocation(PropertyLocation.of(
                command.country(),
                command.city(),
                command.address(),
                command.postalCode()
            ));
        }

        if (command.bedrooms() != null && command.bathrooms() != null && command.maxGuests() != null) {
            property.updateSpecs(PropertySpecs.of(
                command.bedrooms(),
                command.bathrooms(),
                command.maxGuests()
            ));
        }

        if (command.amenities() != null) {
            command.amenities().forEach(property::addAmenity);
        }

        return propertyRepository.save(property);
    }
}
