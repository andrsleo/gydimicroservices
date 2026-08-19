package com.affiliate.rentals.gydi.properties.application.usecase;

import com.affiliate.rentals.gydi.properties.application.service.AirbnbUrlResolver;
import com.affiliate.rentals.gydi.properties.application.service.AirbnbUrlValidator;
import com.affiliate.rentals.gydi.properties.domain.model.*;
import com.affiliate.rentals.gydi.properties.domain.ports.in.UpdatePropertyUseCase;
import com.affiliate.rentals.gydi.properties.domain.ports.out.PropertyRepositoryPort;
import com.affiliate.rentals.gydi.shared.security.HTMLSanitizer;
import com.affiliate.rentals.gydi.shared.util.SlugGenerator;
import org.hashids.Hashids;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UpdatePropertyUseCaseImpl implements UpdatePropertyUseCase {

    private final PropertyRepositoryPort propertyRepository;
    private final HTMLSanitizer htmlSanitizer;
    private final SlugGenerator slugGenerator;
    private final Hashids hashids;
    private final AirbnbUrlResolver urlResolver;
    private final AirbnbUrlValidator urlValidator;
    private final com.affiliate.rentals.gydi.properties.application.service.ICalUrlValidator iCalUrlValidator;

    public UpdatePropertyUseCaseImpl(
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
    public Property updateProperty(UpdatePropertyCommand command) {
        Property property = propertyRepository.findById(command.propertyId())
                .orElseThrow(() -> new IllegalArgumentException("Property not found: " + command.propertyId()));

        if (!property.isOwnedBy(command.requestingUserId())) {
            throw new SecurityException("User is not authorized to update this property");
        }

        // Handle Airbnb URL update
        boolean airbnbUrlChanging = false;
        if (command.airbnbUrl() != null && !command.airbnbUrl().isBlank()) {
            String resolvedUrl = urlResolver.resolve(command.airbnbUrl());
            AirbnbUrlValidator.AirbnbUrlValidationResult validationResult = urlValidator.validate(resolvedUrl);

            if (!validationResult.isValid()) {
                throw new IllegalArgumentException("Invalid Airbnb URL: " + validationResult.getErrorMessage());
            }

            String listingId = validationResult.getListingId();

            // Check if listing ID changed and if the new one already exists on another property
            if (!listingId.equals(property.getAirbnbListingId()) &&
                    propertyRepository.existsByAirbnbListingId(listingId)) {
                throw new IllegalStateException(
                        "Property with Airbnb listing ID " + listingId + " already exists");
            }

            // Track whether the Airbnb URL actually changed
            airbnbUrlChanging = !validationResult.getNormalizedUrl().equals(property.getAirbnbUrl());

            // Update Airbnb fields using setter (avoids data loss from builder rebuild)
            property.updateAirbnbUrl(validationResult.getNormalizedUrl(), listingId);
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
                    command.postalCode()));
        }

        if (command.bedrooms() != null && command.bathrooms() != null && command.maxGuests() != null) {
            property.updateSpecs(PropertySpecs.of(
                    command.bedrooms(),
                    command.bathrooms(),
                    command.maxGuests()));
        }

        // Update property type if provided
        if (command.propertyType() != null && !command.propertyType().isBlank()) {
            property.updatePropertyType(PropertyType.valueOf(command.propertyType()));
        }

        // Update amenities - replace all existing amenities with new ones
        if (command.amenities() != null) {
            // Clear existing amenities
            property.clearAmenities();
            // Add new amenities
            command.amenities().forEach(property::addAmenity);
        }

        boolean icalUrlChanging = false;
        if (command.icalUrlAirbnb() != null) {
            // Validate iCal URL by fetching and verifying content
            if (!command.icalUrlAirbnb().isBlank()) {
                // Use validateWithFetch to ensure the URL actually contains valid iCal data
                com.affiliate.rentals.gydi.properties.application.service.ICalUrlValidator.ICalValidationResult icalResult = iCalUrlValidator
                        .validateWithFetch(command.icalUrlAirbnb());

                if (!icalResult.isValid()) {
                    throw new IllegalArgumentException("Invalid iCal URL: " + icalResult.getMessage());
                }

                // Track whether the iCal URL actually changed
                String currentIcalUrl = property.getIcalUrlAirbnb();
                icalUrlChanging = !command.icalUrlAirbnb().equals(currentIcalUrl);

                // Set the validated URL
                property.updateIcalUrlAirbnb(command.icalUrlAirbnb());
            } else {
                // Clear the URL if empty/blank
                property.updateIcalUrlAirbnb(null);
            }
        }

        // If the Airbnb URL or iCal URL changed on a published/inactive property, require
        // the host to re-add GYDI as co-host on the new Airbnb listing before re-submitting.
        if ((airbnbUrlChanging || icalUrlChanging) &&
                (property.getStatus() == com.affiliate.rentals.gydi.properties.domain.model.PropertyStatus.PUBLISHED
                        || property.getStatus() == com.affiliate.rentals.gydi.properties.domain.model.PropertyStatus.INACTIVE)) {
            property.transitionToSendGydiCohost();
        }

        // Auto-transition DRAFT → SEND_GYDI_COHOST when minimum content requirements are met.
        // This allows the host to proceed with adding GYDI as co-host on Airbnb before submitting.
        if (property.getStatus() == com.affiliate.rentals.gydi.properties.domain.model.PropertyStatus.DRAFT
                && property.meetsSubmitConditions()) {
            property.transitionToSendGydiCohost();
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
