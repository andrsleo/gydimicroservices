package com.affiliate.rentals.gydi.properties.application.usecase;

import com.affiliate.rentals.gydi.properties.domain.model.*;
import com.affiliate.rentals.gydi.properties.domain.ports.in.CreatePropertyUseCase;
import com.affiliate.rentals.gydi.properties.domain.ports.out.PropertyRepositoryPort;
import com.affiliate.rentals.gydi.shared.security.HTMLSanitizer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Currency;

@Service
@Transactional
public class CreatePropertyUseCaseImpl implements CreatePropertyUseCase {

    private final PropertyRepositoryPort propertyRepository;
    private final HTMLSanitizer htmlSanitizer;

    public CreatePropertyUseCaseImpl(PropertyRepositoryPort propertyRepository, HTMLSanitizer htmlSanitizer) {
        this.propertyRepository = propertyRepository;
        this.htmlSanitizer = htmlSanitizer;
    }

    @Override
    public Property createProperty(CreatePropertyCommand command) {
        // Default to SHORT_TERM_RENTAL if listingType is not provided
        PropertyListingType listingType = (command.listingType() != null && !command.listingType().isBlank())
                ? PropertyListingType.valueOf(command.listingType())
                : PropertyListingType.SHORT_TERM_RENTAL;

        // SECURITY: XSS Prevention - Sanitize user-provided text fields
        String sanitizedTitle = htmlSanitizer.sanitizeToPlainText(command.title());
        String sanitizedDescription = htmlSanitizer.sanitizeBasicFormatting(command.description());

        Property.Builder builder = Property.builder()
                .id(PropertyId.generate())
                .hostId(command.hostId())
                .title(sanitizedTitle)
                .description(sanitizedDescription)
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
                .listingType(listingType)
                .status(PropertyStatus.DRAFT);

        // Set sale price if provided
        if (command.salePrice() != null) {
            builder.salePrice(Money.of(command.salePrice(), command.priceCurrency()));
        }

        Property property = builder.build();

        return propertyRepository.save(property);
    }
}
