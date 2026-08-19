package com.affiliate.rentals.gydi.properties.infrastructure.in.rest.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.affiliate.rentals.gydi.properties.application.dto.ImportPropertyFromAirbnbRequest;
import com.affiliate.rentals.gydi.properties.application.dto.PropertyDetailResponse;
import com.affiliate.rentals.gydi.properties.application.dto.ValidateAirbnbUrlRequest;
import com.affiliate.rentals.gydi.properties.application.dto.ValidateAirbnbUrlResponse;
import com.affiliate.rentals.gydi.properties.application.mapper.PropertyMapper;
import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.ports.in.ImportPropertyFromAirbnbUseCase;
import com.affiliate.rentals.gydi.properties.domain.ports.in.ImportPropertyFromAirbnbUseCase.ImportPropertyFromAirbnbCommand;
import com.affiliate.rentals.gydi.properties.domain.ports.in.ImportPropertyFromAirbnbUseCase.ValidateAirbnbUrlCommand;
import com.affiliate.rentals.gydi.properties.domain.ports.in.ImportPropertyFromAirbnbUseCase.ValidateAirbnbUrlResult;
import com.affiliate.rentals.gydi.shared.security.JwtService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * REST controller for Airbnb property import functionality.
 * Provides endpoints for validating Airbnb URLs and importing property data.
 */
@RestController
@RequestMapping("/api/properties/import")
public class PropertyImportController {

    private final ImportPropertyFromAirbnbUseCase importPropertyFromAirbnbUseCase;
    private final PropertyMapper mapper;
    private final JwtService jwtService;

    public PropertyImportController(
            ImportPropertyFromAirbnbUseCase importPropertyFromAirbnbUseCase,
            PropertyMapper mapper,
            JwtService jwtService) {
        this.importPropertyFromAirbnbUseCase = importPropertyFromAirbnbUseCase;
        this.mapper = mapper;
        this.jwtService = jwtService;
    }

    /**
     * Validates an Airbnb URL and extracts metadata for preview.
     *
     * @param request the validation request containing the Airbnb URL
     * @return validation result with extracted metadata if valid
     */
    @PostMapping("/validate")
    public ResponseEntity<ValidateAirbnbUrlResponse> validateAirbnbUrl(
            @Valid @RequestBody ValidateAirbnbUrlRequest request) {

        ValidateAirbnbUrlCommand command = new ValidateAirbnbUrlCommand(request.airbnbUrl());
        ValidateAirbnbUrlResult result = importPropertyFromAirbnbUseCase.validateAirbnbUrl(command);

        ValidateAirbnbUrlResponse response = new ValidateAirbnbUrlResponse(
                result.valid(),
                result.listingId(),
                result.title(),
                result.description(),
                result.imageUrl(),
                result.errorMessage());

        return ResponseEntity.ok(response);
    }

    /**
     * Imports a property from Airbnb by extracting metadata from the listing page.
     * Creates a new property with SEMI_IMPORT mode and extracted data.
     *
     * @param request     the import request containing the Airbnb URL and required
     *                    property fields
     * @param httpRequest the HTTP request for JWT extraction
     * @return the created property with imported data
     */
    @PostMapping("/airbnb")
    public ResponseEntity<PropertyDetailResponse> importFromAirbnb(
            @Valid @RequestBody ImportPropertyFromAirbnbRequest request,
            HttpServletRequest httpRequest) {

        Long userId = jwtService.extractUserIdFromRequest(httpRequest);

        ImportPropertyFromAirbnbCommand command = new ImportPropertyFromAirbnbCommand(
                userId,
                request.airbnbUrl(),
                request.priceAmount(),
                request.priceCurrency(),
                request.country(),
                request.city(),
                request.address(),
                request.postalCode(),
                request.bedrooms(),
                request.bathrooms(),
                request.maxGuests(),
                request.propertyType(),
                request.listingType(),
                request.titleOverride(),
                request.descriptionOverride(),
                request.additionalAmenities());

        Property property = importPropertyFromAirbnbUseCase.importPropertyFromAirbnb(command);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(mapper.toPropertyDetailResponse(property));
    }
}
