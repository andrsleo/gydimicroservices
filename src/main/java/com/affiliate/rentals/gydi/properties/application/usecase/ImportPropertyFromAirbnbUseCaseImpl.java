package com.affiliate.rentals.gydi.properties.application.usecase;

import com.affiliate.rentals.gydi.properties.application.service.AirbnbMetadataExtractor;
import com.affiliate.rentals.gydi.properties.application.service.AirbnbUrlValidator;
import com.affiliate.rentals.gydi.properties.domain.model.*;
import com.affiliate.rentals.gydi.properties.domain.ports.in.ImportPropertyFromAirbnbUseCase;
import com.affiliate.rentals.gydi.properties.domain.ports.out.PropertyRepositoryPort;
import com.affiliate.rentals.gydi.shared.security.HTMLSanitizer;
import com.affiliate.rentals.gydi.shared.util.SlugGenerator;
import org.hashids.Hashids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;

/**
 * Implementation of ImportPropertyFromAirbnbUseCase.
 *
 * This use case orchestrates the process of importing property data from
 * Airbnb:
 * 1. Validates the Airbnb URL
 * 2. Extracts metadata (title, description, images)
 * 3. Creates a property with SEMI_IMPORT mode
 * 4. Associates the Airbnb URL and listing ID
 */
@Service
@Transactional
public class ImportPropertyFromAirbnbUseCaseImpl implements ImportPropertyFromAirbnbUseCase {

    private static final Logger log = LoggerFactory.getLogger(ImportPropertyFromAirbnbUseCaseImpl.class);

    private final AirbnbUrlValidator urlValidator;
    private final AirbnbMetadataExtractor metadataExtractor;
    private final com.affiliate.rentals.gydi.properties.application.service.AirbnbUrlResolver urlResolver;
    private final PropertyRepositoryPort propertyRepository;
    private final HTMLSanitizer htmlSanitizer;
    private final SlugGenerator slugGenerator;
    private final Hashids hashids;

    public ImportPropertyFromAirbnbUseCaseImpl(
            AirbnbUrlValidator urlValidator,
            AirbnbMetadataExtractor metadataExtractor,
            com.affiliate.rentals.gydi.properties.application.service.AirbnbUrlResolver urlResolver,
            PropertyRepositoryPort propertyRepository,
            HTMLSanitizer htmlSanitizer,
            SlugGenerator slugGenerator,
            Hashids hashids) {
        this.urlValidator = urlValidator;
        this.metadataExtractor = metadataExtractor;
        this.urlResolver = urlResolver;
        this.propertyRepository = propertyRepository;
        this.htmlSanitizer = htmlSanitizer;
        this.slugGenerator = slugGenerator;
        this.hashids = hashids;
    }

    @Override
    public ValidateAirbnbUrlResult validateAirbnbUrl(ValidateAirbnbUrlCommand command) {
        log.info("Validating Airbnb URL");

        // 0. Resolve URL if needed (handles mobile/short links)
        String resolvedUrl = urlResolver.resolve(command.airbnbUrl());

        // 1. Validate URL format and extract listing ID
        AirbnbUrlValidator.AirbnbUrlValidationResult validationResult = urlValidator.validate(resolvedUrl);

        if (!validationResult.isValid()) {
            log.warn("Invalid Airbnb URL: {}", validationResult.getErrorMessage());
            return ValidateAirbnbUrlResult.invalid(validationResult.getErrorMessage());
        }

        // 2. Check if listing ID already exists
        String listingId = validationResult.getListingId();
        if (propertyRepository.existsByAirbnbListingId(listingId)) {
            log.warn("Property with Airbnb listing ID {} already exists", listingId);
            return ValidateAirbnbUrlResult.invalid(
                    "This Airbnb listing has already been imported. Listing ID: " + listingId);
        }

        // 3. Extract metadata (title, description, image)
        try {
            AirbnbMetadataExtractor.AirbnbMetadata metadata = metadataExtractor
                    .extract(validationResult.getNormalizedUrl());

            log.info("Successfully validated Airbnb URL and extracted metadata. Listing ID: {}", listingId);

            return ValidateAirbnbUrlResult.valid(
                    listingId,
                    metadata.getTitle(),
                    metadata.getDescription(),
                    metadata.getImageUrl(),
                    metadata.getBedrooms(),
                    metadata.getBathrooms(),
                    metadata.getMaxGuests(),
                    metadata.getAmenities(),
                    metadata.getCity(),
                    metadata.getCountry(),
                    metadata.getAddress());
        } catch (AirbnbMetadataExtractor.AirbnbMetadataExtractionException e) {
            log.error("Failed to extract metadata from Airbnb listing: {}", e.getMessage());
            return ValidateAirbnbUrlResult.invalid(
                    "Could not extract data from Airbnb. Please try again or enter data manually.");
        }
    }

    @Override
    public Property importPropertyFromAirbnb(ImportPropertyFromAirbnbCommand command) {
        log.info("Importing property from Airbnb for host ID: {}", command.hostId());

        // 0. Resolve URL if needed
        String resolvedUrl = urlResolver.resolve(command.airbnbUrl());

        // 1. Validate URL and extract listing ID
        AirbnbUrlValidator.AirbnbUrlValidationResult validationResult = urlValidator.validate(resolvedUrl);

        if (!validationResult.isValid()) {
            throw new IllegalArgumentException("Invalid Airbnb URL: " + validationResult.getErrorMessage());
        }

        String listingId = validationResult.getListingId();
        String normalizedUrl = validationResult.getNormalizedUrl();

        // 2. Check for duplicates
        if (propertyRepository.existsByAirbnbListingId(listingId)) {
            throw new IllegalStateException(
                    "Property with Airbnb listing ID " + listingId + " already exists");
        }

        // 3. Extract metadata
        AirbnbMetadataExtractor.AirbnbMetadata metadata;
        try {
            metadata = metadataExtractor.extract(normalizedUrl);
        } catch (AirbnbMetadataExtractor.AirbnbMetadataExtractionException e) {
            log.warn("Failed to extract metadata, will use user-provided data only: {}", e.getMessage());
            metadata = AirbnbMetadataExtractor.AirbnbMetadata.builder().build();
        }

        // 4. Determine title and description (use imported or override)
        String finalTitle = determineFinalTitle(command, metadata);
        String finalDescription = determineFinalDescription(command, metadata);

        // 5. Sanitize user input
        String sanitizedTitle = htmlSanitizer.sanitizeToPlainText(finalTitle);
        String sanitizedDescription = htmlSanitizer.sanitizeBasicFormatting(finalDescription);

        // 6. Generate slug
        String tempShortId = generateTemporaryShortId();
        String slug = slugGenerator.generateUniqueSlug(sanitizedTitle, tempShortId);

        // 7. Build property with import mode
        PropertyListingType listingType = command.listingType() != null && !command.listingType().isBlank()
                ? PropertyListingType.valueOf(command.listingType())
                : PropertyListingType.SHORT_TERM_RENTAL;

        Property.Builder builder = Property.builder()
                .hostId(command.hostId())
                .title(sanitizedTitle)
                .slug(slug)
                .description(sanitizedDescription)
                .pricePerNight(Money.of(command.priceAmount(), command.priceCurrency()))
                .location(PropertyLocation.of(
                        command.country(),
                        command.city(),
                        command.address(),
                        command.postalCode()))
                .amenities(command.additionalAmenities() != null ? command.additionalAmenities() : new ArrayList<>())
                .specs(PropertySpecs.of(
                        command.bedrooms(),
                        command.bathrooms(),
                        command.maxGuests()))
                .propertyType(PropertyType.valueOf(command.propertyType()))
                .listingType(listingType)
                .status(PropertyStatus.DRAFT)
                // Airbnb import fields
                .airbnbUrl(normalizedUrl)
                .importMode(ImportMode.SEMI_IMPORT)
                .importedAt(LocalDateTime.now())
                .airbnbListingId(listingId);

        Property property = builder.build();

        // 8. Save with temporary slug
        Property savedProperty = propertyRepository.save(property);

        // 9. Update slug with real ID-based short code
        String finalShortId = generateShortId(savedProperty.getId().getValue());
        String finalSlug = slugGenerator.generateUniqueSlug(sanitizedTitle, finalShortId);
        savedProperty.setSlug(finalSlug);

        Property finalProperty = propertyRepository.save(savedProperty);

        log.info("Successfully imported property from Airbnb. Property ID: {}, Listing ID: {}",
                finalProperty.getId().getValue(), listingId);

        return finalProperty;
    }

    /**
     * Determines the final title to use (user override or imported).
     */
    private String determineFinalTitle(ImportPropertyFromAirbnbCommand command,
            AirbnbMetadataExtractor.AirbnbMetadata metadata) {
        if (command.titleOverride() != null && !command.titleOverride().isBlank()) {
            return command.titleOverride();
        }

        if (metadata.hasTitle()) {
            return metadata.getTitle();
        }

        throw new IllegalArgumentException(
                "Title is required. Either provide titleOverride or ensure Airbnb metadata extraction succeeds.");
    }

    /**
     * Determines the final description to use (user override or imported).
     */
    private String determineFinalDescription(ImportPropertyFromAirbnbCommand command,
            AirbnbMetadataExtractor.AirbnbMetadata metadata) {
        if (command.descriptionOverride() != null && !command.descriptionOverride().isBlank()) {
            return command.descriptionOverride();
        }

        if (metadata.hasDescription()) {
            return metadata.getDescription();
        }

        // Description can be null (not required for DRAFT)
        return null;
    }

    /**
     * Generates a short ID from Long ID using Hashids.
     */
    private String generateShortId(Long id) {
        return hashids.encode(Math.abs(id));
    }

    /**
     * Generates a temporary short ID using timestamp for uniqueness.
     */
    private String generateTemporaryShortId() {
        long timestamp = System.currentTimeMillis();
        return hashids.encode(timestamp % 1000000);
    }
}
