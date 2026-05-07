package com.affiliate.rentals.gydi.properties.application.usecase;

import com.affiliate.rentals.gydi.properties.application.service.AirbnbUrlResolver;
import com.affiliate.rentals.gydi.properties.application.service.AirbnbUrlValidator;
import com.affiliate.rentals.gydi.properties.domain.model.*;
import com.affiliate.rentals.gydi.properties.domain.ports.in.CreatePropertyUseCase;
import com.affiliate.rentals.gydi.properties.domain.ports.out.PropertyRepositoryPort;
import com.affiliate.rentals.gydi.shared.security.HTMLSanitizer;
import com.affiliate.rentals.gydi.shared.util.SlugGenerator;
import org.hashids.Hashids;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Service
@Transactional
public class CreatePropertyUseCaseImpl implements CreatePropertyUseCase {

    private final PropertyRepositoryPort propertyRepository;
    private final HTMLSanitizer htmlSanitizer;
    private final SlugGenerator slugGenerator;
    private final Hashids hashids;
    private final AirbnbUrlResolver urlResolver;
    private final AirbnbUrlValidator urlValidator;
    private final com.affiliate.rentals.gydi.properties.application.service.ICalUrlValidator iCalUrlValidator;

    public CreatePropertyUseCaseImpl(
            PropertyRepositoryPort propertyRepository,
            HTMLSanitizer htmlSanitizer,
            SlugGenerator slugGenerator,
            Hashids hashids,
            AirbnbUrlResolver urlResolver,
            AirbnbUrlValidator urlValidator,
            com.affiliate.rentals.gydi.properties.application.service.ICalUrlValidator iCalUrlValidator) {
        this.propertyRepository = propertyRepository;
        this.htmlSanitizer = htmlSanitizer;
        this.slugGenerator = slugGenerator;
        this.hashids = hashids;
        this.urlResolver = urlResolver;
        this.urlValidator = urlValidator;
        this.iCalUrlValidator = iCalUrlValidator;
    }

    @Override
    public Property createProperty(CreatePropertyCommand command) {
        // 1. Resolve and validate Airbnb URL
        String resolvedUrl = urlResolver.resolve(command.airbnbUrl());
        AirbnbUrlValidator.AirbnbUrlValidationResult validationResult = urlValidator.validate(resolvedUrl);

        if (!validationResult.isValid()) {
            throw new IllegalArgumentException("Invalid Airbnb URL: " + validationResult.getErrorMessage());
        }

        String listingId = validationResult.getListingId();

        // 2. Validate iCal URL if provided
        if (command.icalUrlAirbnb() != null && !command.icalUrlAirbnb().isBlank()) {
            // Use validateWithFetch to ensure the URL actually contains valid iCal data
            com.affiliate.rentals.gydi.properties.application.service.ICalUrlValidator.ICalValidationResult icalResult = iCalUrlValidator
                    .validateWithFetch(command.icalUrlAirbnb());

            if (!icalResult.isValid()) {
                throw new IllegalArgumentException("Invalid iCal URL: " + icalResult.getMessage());
            }
        }

        // 3. Check if listing ID already exists
        if (propertyRepository.existsByAirbnbListingId(listingId)) {
            throw new IllegalStateException(
                    "Property with Airbnb listing ID " + listingId + " already exists");
        }

        // 3. Default to SHORT_TERM_RENTAL if listingType is not provided
        PropertyListingType listingType = (command.listingType() != null && !command.listingType().isBlank())
                ? PropertyListingType.valueOf(command.listingType())
                : PropertyListingType.SHORT_TERM_RENTAL;

        // Validate conditional price requirements (belt-and-suspenders; frontend schema also enforces this)
        if ((listingType == PropertyListingType.SHORT_TERM_RENTAL || listingType == PropertyListingType.BOTH)
                && command.priceAmount() == null) {
            throw new IllegalArgumentException("pricePerNight is required for listing type: " + listingType);
        }
        if ((listingType == PropertyListingType.SALE || listingType == PropertyListingType.BOTH)
                && command.salePrice() == null) {
            throw new IllegalArgumentException("salePrice is required for listing type: " + listingType);
        }

        // 4. SECURITY: XSS Prevention - Sanitize user-provided text fields
        String sanitizedTitle = htmlSanitizer.sanitizeToPlainText(command.title());
        String sanitizedDescription = htmlSanitizer.sanitizeBasicFormatting(command.description());

        // 5. Generate temporary unique slug using timestamp + random (will be updated
        // after save)
        String tempShortId = generateTemporaryShortId();
        String slug = slugGenerator.generateUniqueSlug(sanitizedTitle, tempShortId);

        Property.Builder builder = Property.builder()
                .hostId(command.hostId())
                .title(sanitizedTitle)
                .slug(slug) // Set slug BEFORE first save
                .description(sanitizedDescription)
                .pricePerNight(Money.of(command.priceAmount(), command.priceCurrency()))
                .location(PropertyLocation.of(
                        command.country(),
                        command.city(),
                        command.address(),
                        command.postalCode()))
                .amenities(command.amenities() != null ? command.amenities() : new ArrayList<>())
                .specs(PropertySpecs.of(
                        command.bedrooms(),
                        command.bathrooms(),
                        command.maxGuests()))
                .propertyType(PropertyType.valueOf(command.propertyType()))
                .listingType(listingType)
                .status(PropertyStatus.DRAFT)
                // Airbnb fields
                .airbnbUrl(validationResult.getNormalizedUrl())
                .airbnbUrl(validationResult.getNormalizedUrl())
                .airbnbListingId(listingId)
                .icalUrlAirbnb(command.icalUrlAirbnb());
        // Note: importMode and importedAt are left as NULL

        // 6. Set sale price if provided
        if (command.salePrice() != null) {
            builder.salePrice(Money.of(command.salePrice(), command.priceCurrency()));
        }

        Property property = builder.build();

        // 7. Save with temporary slug
        Property savedProperty = propertyRepository.save(property);

        // 8. Update slug with real ID-based short code
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
