package com.affiliate.rentals.gydi.properties.application.usecase;

import com.affiliate.rentals.gydi.properties.domain.model.*;
import com.affiliate.rentals.gydi.properties.domain.ports.in.CreatePropertyUseCase;
import com.affiliate.rentals.gydi.properties.domain.ports.out.PropertyRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Currency;

@Service
@Transactional
public class CreatePropertyUseCaseImpl implements CreatePropertyUseCase {

    private final PropertyRepositoryPort propertyRepository;

    public CreatePropertyUseCaseImpl(PropertyRepositoryPort propertyRepository) {
        this.propertyRepository = propertyRepository;
    }

    @Override
    public Property createProperty(CreatePropertyCommand command) {
        Property property = Property.builder()
                .id(PropertyId.generate())
                .hostId(command.hostId())
                .title(command.title())
                .description(command.description())
                .pricePerNight(Money.of(command.priceAmount(), command.priceCurrency()))
                .location(PropertyLocation.of(
                    command.country(),
                    command.city(),
                    command.address(),
                    command.postalCode()
                ))
                .amenities(command.amenities() != null ? command.amenities() : new ArrayList<>())
                .specs(PropertySpecs.of(
                    command.bedrooms(),
                    command.bathrooms(),
                    command.maxGuests()
                ))
                .propertyType(PropertyType.valueOf(command.propertyType()))
                .status(PropertyStatus.DRAFT)
                .build();

        return propertyRepository.save(property);
    }
}
