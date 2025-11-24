package com.affiliate.rentals.gydi.properties.application.usecase;

import com.affiliate.rentals.gydi.properties.domain.model.*;
import com.affiliate.rentals.gydi.properties.domain.ports.in.UpdatePropertyUseCase;
import com.affiliate.rentals.gydi.properties.domain.ports.out.PropertyRepositoryPort;
import com.affiliate.rentals.gydi.shared.security.HTMLSanitizer;
import com.affiliate.rentals.gydi.shared.util.SlugGenerator;
import org.hashids.Hashids;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Currency;

@Service
@Transactional
public class UpdatePropertyUseCaseImpl implements UpdatePropertyUseCase {

    private final PropertyRepositoryPort propertyRepository;
    private final HTMLSanitizer htmlSanitizer;
    private final SlugGenerator slugGenerator;
    private final Hashids hashids;

    public UpdatePropertyUseCaseImpl(
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
    public Property updateProperty(UpdatePropertyCommand command) {
        Property property = propertyRepository.findById(command.propertyId())
                .orElseThrow(() -> new IllegalArgumentException("Property not found: " + command.propertyId()));

        if (!property.isOwnedBy(command.requestingUserId())) {
            throw new SecurityException("User is not authorized to update this property");
        }

        // Update listing type first if provided
        if (command.listingType() != null && !command.listingType().isBlank()) {
            property.updateListingType(PropertyListingType.valueOf(command.listingType()));
        }

        if (command.title() != null || command.description() != null ||
            command.priceAmount() != null || command.salePrice() != null) {

            // SECURITY: XSS Prevention - Sanitize user-provided text fields
            String sanitizedTitle = command.title() != null
                ? htmlSanitizer.sanitizeToPlainText(command.title())
                : null;
            String sanitizedDescription = command.description() != null
                ? htmlSanitizer.sanitizeBasicFormatting(command.description())
                : null;

            Money newPrice = command.priceAmount() != null && command.priceCurrency() != null
                ? Money.of(command.priceAmount(), command.priceCurrency())
                : property.getPricePerNight();
            Money newSalePrice = command.salePrice() != null && command.priceCurrency() != null
                ? Money.of(command.salePrice(), command.priceCurrency())
                : property.getSalePrice(); // Preserve current value if not provided
            property.updateDetails(sanitizedTitle, sanitizedDescription, newPrice, newSalePrice);

            // Regenerate slug if title changed
            if (sanitizedTitle != null && !sanitizedTitle.equals(property.getTitle())) {
                String shortId = generateShortId(property.getId().getValue());
                String newSlug = slugGenerator.generateUniqueSlug(sanitizedTitle, shortId);
                property.setSlug(newSlug);
            }
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

    /**
     * Generates a short ID from Long ID using Hashids.
     */
    private String generateShortId(Long id) {
        // Hashids requires positive numbers
        return hashids.encode(Math.abs(id));
    }
}
