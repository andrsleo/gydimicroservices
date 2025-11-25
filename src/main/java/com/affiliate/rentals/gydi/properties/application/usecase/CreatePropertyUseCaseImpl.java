package com.affiliate.rentals.gydi.properties.application.usecase;

import com.affiliate.rentals.gydi.properties.domain.model.*;
import com.affiliate.rentals.gydi.properties.domain.ports.in.CreatePropertyUseCase;
import com.affiliate.rentals.gydi.properties.domain.ports.out.PropertyRepositoryPort;
import com.affiliate.rentals.gydi.shared.security.HTMLSanitizer;
import com.affiliate.rentals.gydi.shared.util.SlugGenerator;
import org.hashids.Hashids;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Currency;

@Service
@Transactional
public class CreatePropertyUseCaseImpl implements CreatePropertyUseCase {

    private final PropertyRepositoryPort propertyRepository;
    private final HTMLSanitizer htmlSanitizer;
    private final SlugGenerator slugGenerator;
    private final Hashids hashids;

    public CreatePropertyUseCaseImpl(
            PropertyRepositoryPort propertyRepository,
            HTMLSanitizer htmlSanitizer,
            SlugGenerator slugGenerator,
            Hashids hashids) {
        this.propertyRepository = propertyRepository;
        this.htmlSanitizer = htmlSanitizer;
        this.slugGenerator = slugGenerator;
        this.hashids = hashids;
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

        // Generate temporary unique slug using timestamp + random (will be updated after save)
        String tempShortId = generateTemporaryShortId();
        String slug = slugGenerator.generateUniqueSlug(sanitizedTitle, tempShortId);

        Property.Builder builder = Property.builder()
                .hostId(command.hostId())
                .title(sanitizedTitle)
                .slug(slug)  // Set slug BEFORE first save
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

        // Save with temporary slug
        Property savedProperty = propertyRepository.save(property);

        // Update slug with real ID-based short code
        String finalShortId = generateShortId(savedProperty.getId().getValue());
        String finalSlug = slugGenerator.generateUniqueSlug(sanitizedTitle, finalShortId);
        savedProperty.setSlug(finalSlug);

        return propertyRepository.save(savedProperty);
    }

    /**
     * Generates a short ID from Long ID using Hashids.
     */
    private String generateShortId(Long id) {
        // Hashids requires positive numbers
        return hashids.encode(Math.abs(id));
    }

    /**
     * Generates a temporary short ID using timestamp for uniqueness.
     * This is used before the property ID is available.
     * Format: tmp-{timestamp-in-base36}
     */
    private String generateTemporaryShortId() {
        // Use current timestamp in milliseconds, convert to base 36 for shorter string
        long timestamp = System.currentTimeMillis();
        return "tmp-" + Long.toString(timestamp, 36);
    }
}
