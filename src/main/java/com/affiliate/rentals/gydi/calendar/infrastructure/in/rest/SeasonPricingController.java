package com.affiliate.rentals.gydi.calendar.infrastructure.in.rest;

import com.affiliate.rentals.gydi.calendar.application.dto.SeasonDefinitionDto;
import com.affiliate.rentals.gydi.calendar.application.dto.SeasonPricingResponse;
import com.affiliate.rentals.gydi.calendar.application.dto.UpdateSeasonPricingRequest;
import com.affiliate.rentals.gydi.calendar.domain.port.in.GetSeasonPricingUseCase;
import com.affiliate.rentals.gydi.calendar.domain.port.in.GetSeasonsByCountryUseCase;
import com.affiliate.rentals.gydi.calendar.domain.port.in.UpdateSeasonPricingUseCase;
import com.affiliate.rentals.gydi.shared.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for season-based pricing management.
 *
 * <p>Provides endpoints to retrieve and update season multipliers configured
 * for a property, as well as listing the pre-defined season definitions
 * available for a given country.</p>
 *
 * <p>Season pricing allows hosts to apply price multipliers during high-demand
 * periods (e.g., summer, holidays) relative to the property's base price.</p>
 */
@RestController
@Tag(name = "Season Pricing", description = "Season multipliers and season definitions")
public class SeasonPricingController {

    private static final Logger log = LoggerFactory.getLogger(SeasonPricingController.class);

    private final GetSeasonPricingUseCase getSeasonPricingUseCase;
    private final UpdateSeasonPricingUseCase updateSeasonPricingUseCase;
    private final GetSeasonsByCountryUseCase getSeasonsByCountryUseCase;

    public SeasonPricingController(
            GetSeasonPricingUseCase getSeasonPricingUseCase,
            UpdateSeasonPricingUseCase updateSeasonPricingUseCase,
            GetSeasonsByCountryUseCase getSeasonsByCountryUseCase) {
        this.getSeasonPricingUseCase = getSeasonPricingUseCase;
        this.updateSeasonPricingUseCase = updateSeasonPricingUseCase;
        this.getSeasonsByCountryUseCase = getSeasonsByCountryUseCase;
    }

    /**
     * Retrieves the season pricing configuration for a property.
     *
     * <p>Requires authentication (any role).</p>
     *
     * @param propertyId the property identifier
     * @return the current season pricing configuration for the property
     */
    @GetMapping("/api/v1/properties/{propertyId}/season-pricing")
    @Operation(
            summary = "Get season pricing for a property",
            description = "Returns the configured season multipliers for the property. Requires authentication.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Season pricing retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Property not found")
    })
    public ResponseEntity<SeasonPricingResponse> getSeasonPricing(
            @PathVariable Long propertyId) {
        log.debug("GET season pricing for propertyId={}", propertyId);
        SeasonPricingResponse response = getSeasonPricingUseCase.execute(propertyId);
        return ResponseEntity.ok(response);
    }

    /**
     * Updates the season pricing configuration for a property.
     *
     * <p>HOST role required. Allows a host to configure price multipliers
     * for different seasons (e.g., 1.5x during peak summer).</p>
     *
     * @param propertyId  the property identifier
     * @param request     the updated season pricing configuration
     * @param userDetails the authenticated host
     * @return the updated season pricing configuration
     */
    @PutMapping("/api/v1/properties/{propertyId}/season-pricing")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Update season pricing for a property",
            description = "Configures season multipliers for the property. Only the property owner can update pricing (ownership validated by use case).",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Season pricing updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid pricing configuration"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden — user does not own this property"),
            @ApiResponse(responseCode = "404", description = "Property not found")
    })
    public ResponseEntity<SeasonPricingResponse> updateSeasonPricing(
            @PathVariable Long propertyId,
            @RequestBody @Valid UpdateSeasonPricingRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.debug("PUT season pricing for propertyId={} hostId={}", propertyId, userDetails.getUserId());
        SeasonPricingResponse response = updateSeasonPricingUseCase.execute(
                propertyId, request, userDetails.getUserId());
        return ResponseEntity.ok(response);
    }

    /**
     * Lists the pre-defined season definitions available for a country.
     *
     * <p>Public endpoint — no authentication required. Returns the standard
     * seasons (e.g., High Season, Low Season, Holidays) that can be configured
     * for properties in a given country.</p>
     *
     * @param country the ISO 3166-1 alpha-2 country code (e.g., "MX", "CO", "CL")
     * @return list of season definitions applicable to the given country
     */
    @GetMapping("/api/v1/seasons")
    @Operation(
            summary = "Get season definitions by country",
            description = "Returns the standard season definitions (name, dates, default multiplier) available for a country. Public endpoint."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Season definitions retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or unsupported country code")
    })
    public ResponseEntity<List<SeasonDefinitionDto>> getSeasonsByCountry(
            @RequestParam String country) {
        log.debug("GET seasons by country={}", country);
        List<SeasonDefinitionDto> seasons = getSeasonsByCountryUseCase.execute(country);
        return ResponseEntity.ok(seasons);
    }
}
