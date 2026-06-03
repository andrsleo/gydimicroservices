package com.affiliate.rentals.gydi.calendar.infrastructure.in.rest;

import com.affiliate.rentals.gydi.calendar.application.dto.BlockDatesRequest;
import com.affiliate.rentals.gydi.calendar.application.dto.CalendarMonthResponse;
import com.affiliate.rentals.gydi.calendar.application.dto.PriceBreakdownResponse;
import com.affiliate.rentals.gydi.calendar.application.dto.SetCustomPriceRequest;
import com.affiliate.rentals.gydi.calendar.domain.port.in.BlockDatesUseCase;
import com.affiliate.rentals.gydi.calendar.domain.port.in.CalculatePriceUseCase;
import com.affiliate.rentals.gydi.calendar.domain.port.in.GetCalendarUseCase;
import com.affiliate.rentals.gydi.calendar.domain.port.in.RemoveCustomPriceUseCase;
import com.affiliate.rentals.gydi.calendar.domain.port.in.SetCustomPriceUseCase;
import com.affiliate.rentals.gydi.calendar.domain.port.in.UnblockDatesUseCase;
import com.affiliate.rentals.gydi.properties.domain.model.Money;
import com.affiliate.rentals.gydi.shared.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * REST controller for property calendar management.
 *
 * <p>Provides endpoints for viewing the calendar, blocking/unblocking dates,
 * setting custom prices on specific dates, and calculating the total price
 * for a date range.</p>
 *
 * <p>Public read endpoints are accessible without authentication. All mutation
 * endpoints require the HOST role and are further guarded by method-level
 * {@code @PreAuthorize} annotations.</p>
 */
@RestController
@RequestMapping("/api/v1/properties/{propertyId}/calendar")
@Tag(name = "Property Calendar", description = "Calendar management, custom pricing, and price calculation")
public class CalendarController {

    private static final Logger log = LoggerFactory.getLogger(CalendarController.class);

    private final GetCalendarUseCase getCalendarUseCase;
    private final BlockDatesUseCase blockDatesUseCase;
    private final UnblockDatesUseCase unblockDatesUseCase;
    private final SetCustomPriceUseCase setCustomPriceUseCase;
    private final RemoveCustomPriceUseCase removeCustomPriceUseCase;
    private final CalculatePriceUseCase calculatePriceUseCase;

    public CalendarController(
            GetCalendarUseCase getCalendarUseCase,
            BlockDatesUseCase blockDatesUseCase,
            UnblockDatesUseCase unblockDatesUseCase,
            SetCustomPriceUseCase setCustomPriceUseCase,
            RemoveCustomPriceUseCase removeCustomPriceUseCase,
            CalculatePriceUseCase calculatePriceUseCase) {
        this.getCalendarUseCase = getCalendarUseCase;
        this.blockDatesUseCase = blockDatesUseCase;
        this.unblockDatesUseCase = unblockDatesUseCase;
        this.setCustomPriceUseCase = setCustomPriceUseCase;
        this.removeCustomPriceUseCase = removeCustomPriceUseCase;
        this.calculatePriceUseCase = calculatePriceUseCase;
    }

    /**
     * Retrieves the calendar for a property for a given year and month.
     *
     * <p>Public endpoint — no authentication required.</p>
     *
     * @param propertyId the property identifier
     * @param year       the calendar year (e.g., 2026)
     * @param month      the calendar month (1–12)
     * @return the calendar month response with availability and pricing per day
     */
    @GetMapping
    @Operation(
            summary = "Get property calendar",
            description = "Returns the availability calendar for a property for the given year and month. Public endpoint."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Calendar retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Property not found")
    })
    public ResponseEntity<CalendarMonthResponse> getCalendar(
            @PathVariable Long propertyId,
            @RequestParam int year,
            @RequestParam int month) {
        log.debug("GET calendar for propertyId={} year={} month={}", propertyId, year, month);
        CalendarMonthResponse response = getCalendarUseCase.execute(propertyId, year, month);
        return ResponseEntity.ok(response);
    }

    /**
     * Blocks one or more dates on the property calendar.
     *
     * <p>HOST role required.</p>
     *
     * @param propertyId  the property identifier
     * @param request     list of dates to block and optional notes
     * @param userDetails the authenticated host
     * @return 204 No Content on success
     */
    @PostMapping("/block")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Block dates",
            description = "Marks one or more dates as blocked (unavailable) on the property calendar. Ownership validated by use case.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Dates blocked successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden — user does not own this property"),
            @ApiResponse(responseCode = "409", description = "One or more dates are already blocked")
    })
    public ResponseEntity<Void> blockDates(
            @PathVariable Long propertyId,
            @RequestBody @Valid BlockDatesRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.debug("POST block dates for propertyId={} hostId={} dates={}",
                propertyId, userDetails.getUserId(), request.dates());
        blockDatesUseCase.execute(propertyId, request.dates(), request.notes(), userDetails.getUserId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Unblocks a single previously blocked date on the property calendar.
     *
     * <p>HOST role required. Dates with active reservations cannot be unblocked.</p>
     *
     * @param propertyId  the property identifier
     * @param date        the date to unblock (ISO format, e.g., 2026-06-15)
     * @param userDetails the authenticated host
     * @return 204 No Content on success
     */
    @DeleteMapping("/block")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Unblock a date",
            description = "Removes the block from a previously blocked date. Dates with active reservations cannot be unblocked.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Date unblocked successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden — user does not own this property"),
            @ApiResponse(responseCode = "404", description = "Date is not blocked"),
            @ApiResponse(responseCode = "409", description = "Cannot unblock a reserved date")
    })
    public ResponseEntity<Void> unblockDate(
            @PathVariable Long propertyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.debug("DELETE unblock date={} for propertyId={} hostId={}", date, propertyId, userDetails.getUserId());
        unblockDatesUseCase.execute(propertyId, date, userDetails.getUserId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Sets a custom price for a specific date on the property calendar.
     *
     * <p>HOST role required. Overrides the base or season price for that date.</p>
     *
     * @param propertyId  the property identifier
     * @param request     the date, amount, and currency for the custom price
     * @param userDetails the authenticated host
     * @return 204 No Content on success
     */
    @PostMapping("/price")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Set custom price for a date",
            description = "Overrides the default or season price with a custom price for a specific date.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Custom price set successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid price amount or currency"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden — user does not own this property")
    })
    public ResponseEntity<Void> setCustomPrice(
            @PathVariable Long propertyId,
            @RequestBody @Valid SetCustomPriceRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.debug("POST custom price for propertyId={} date={} amount={} currency={} hostId={}",
                propertyId, request.date(), request.amount(), request.currency(), userDetails.getUserId());
        setCustomPriceUseCase.execute(
                propertyId,
                request.date(),
                Money.of(request.amount(), request.currency()),
                userDetails.getUserId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Removes the custom price for a specific date, reverting to the default pricing.
     *
     * <p>HOST role required.</p>
     *
     * @param propertyId  the property identifier
     * @param date        the date whose custom price should be removed (ISO format)
     * @param userDetails the authenticated host
     * @return 204 No Content on success
     */
    @DeleteMapping("/price")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Remove custom price for a date",
            description = "Removes the custom price override for a date, reverting to base or season pricing.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Custom price removed successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden — user does not own this property"),
            @ApiResponse(responseCode = "404", description = "No custom price found for this date")
    })
    public ResponseEntity<Void> removeCustomPrice(
            @PathVariable Long propertyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.debug("DELETE custom price date={} for propertyId={} hostId={}", date, propertyId, userDetails.getUserId());
        removeCustomPriceUseCase.execute(propertyId, date, userDetails.getUserId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Calculates the total price for a date range (check-in to check-out).
     *
     * <p>Public endpoint — no authentication required. Applies season multipliers
     * and custom prices for each night in the range.</p>
     *
     * @param propertyId the property identifier
     * @param checkIn    the check-in date (ISO format, e.g., 2026-06-15)
     * @param checkOut   the check-out date (ISO format, e.g., 2026-06-20)
     * @return a detailed price breakdown per night and total
     */
    @GetMapping("/price-range")
    @Operation(
            summary = "Calculate price for a date range",
            description = "Returns a nightly price breakdown and total for the requested check-in/check-out range. Public endpoint."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Price calculated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid date range"),
            @ApiResponse(responseCode = "404", description = "Property not found")
    })
    public ResponseEntity<PriceBreakdownResponse> calculatePrice(
            @PathVariable Long propertyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut) {
        log.debug("GET price-range for propertyId={} checkIn={} checkOut={}", propertyId, checkIn, checkOut);
        PriceBreakdownResponse response = calculatePriceUseCase.execute(propertyId, checkIn, checkOut);
        return ResponseEntity.ok(response);
    }
}
