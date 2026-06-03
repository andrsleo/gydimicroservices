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
        if (command.airbnbUrl() != null && !command.airbnbUrl().isBlank()) {
            String resolvedUrl = urlResolver.resolve(command.airbnbUrl());
            AirbnbUrlValidator.AirbnbUrlValidationResult validationResult = urlValidator.validate(resolvedUrl);

            if (!validationResult.isValid()) {
                throw new IllegalArgumentException("Invalid Airbnb URL: " + validationResult.getErrorMessage());
            }

            String listingId = validationResult.getListingId();

            if (!listingId.equals(property.getAirbnbListingId()) &&
                    propertyRepository.existsByAirbnbListingId(listingId)) {
                throw new IllegalStateException(
                        "Property with Airbnb listing ID " + listingId + " already exists");
            }

            property.updateAirbnbUrl(validationResult.getNormalizedUrl(), listingId);
        }

        // Update listing type first if provided
        if (command.listingType() != null && !command.listingType().isBlank()) {
            property.updateListingType(PropertyListingType.valueOf(command.listingType()));
        }

        if (command.title() != null || command.description() != null ||
                command.priceAmount() != null || command.salePrice() != null) {

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
                    : property.getSalePrice();
            property.updateDetails(sanitizedTitle, sanitizedDescription, newPrice, newSalePrice);

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

        if (command.propertyType() != null && !command.propertyType().isBlank()) {
            property.updatePropertyType(PropertyType.valueOf(command.propertyType()));
        }

        if (command.amenities() != null) {
            property.clearAmenities();
            command.amenities().forEach(property::addAmenity);
        }

        if (command.icalUrlAirbnb() != null) {
            if (!command.icalUrlAirbnb().isBlank()) {
                com.affiliate.rentals.gydi.properties.application.service.ICalUrlValidator.ICalValidationResult icalResult =
                        iCalUrlValidator.validateWithFetch(command.icalUrlAirbnb());

                if (!icalResult.isValid()) {
                    throw new IllegalArgumentException("Invalid iCal URL: " + icalResult.getMessage());
                }

                property.updateIcalUrlAirbnb(command.icalUrlAirbnb());
            } else {
                property.updateIcalUrlAirbnb(null);
            }
        }

        // Auto-transition: DRAFT -> PENDING_APPROVAL when all minimums are met.
        // Also demotes PENDING_APPROVAL back to DRAFT if an edit breaks minimums.
        property.autoTransitionIfReady();
        property.demoteToDraftIfMinimumsBroken();

        return propertyRepository.save(property);
    }

    private String generateShortId(Long id) {
        return hashids.encode(Math.abs(id));
    }
}
