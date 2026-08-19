package com.affiliate.rentals.gydi.bookings.infrastructure.in.rest.controller;

import com.affiliate.rentals.gydi.bookings.application.dto.BookingDto;
import com.affiliate.rentals.gydi.bookings.application.dto.CreateBookingRequest;
import com.affiliate.rentals.gydi.bookings.application.usecase.CreateBookingUseCase;
import com.affiliate.rentals.gydi.shared.exception.TooManyRequestsException;
import com.affiliate.rentals.gydi.shared.security.RateLimitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/**
 * Public REST Controller for Booking operations (no authentication required).
 * <p>
 * Provides endpoints for guests to create booking requests via referral links.
 * </p>
 */
@RestController
@RequestMapping("/api/public/bookings")
@Tag(name = "Public Bookings", description = "Public booking endpoints for guests (no authentication required)")
public class PublicBookingController {
    private static final Logger log = LoggerFactory.getLogger(PublicBookingController.class);

    private final CreateBookingUseCase createBookingUseCase;
    private final RateLimitService rateLimitService;

    public PublicBookingController(
            CreateBookingUseCase createBookingUseCase,
            RateLimitService rateLimitService) {
        this.createBookingUseCase = createBookingUseCase;
        this.rateLimitService = rateLimitService;
    }

    /**
     * Create a new booking request (PUBLIC - no authentication required).
     * <p>
     * Used by guests clicking on affiliate referral links to request a booking.
     * Rate limited to prevent spam (5 requests per 15 minutes per IP).
     * </p>
     *
     * @param request      booking details (dates, guest info, etc.)
     * @param httpRequest  HTTP request for rate limiting by IP
     * @return created booking DTO with booking ID
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Create a new booking request (PUBLIC)",
        description = "Allows guests to create a booking request without authentication. " +
                      "Rate limited to 5 requests per 15 minutes per IP to prevent abuse."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Booking created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data (validation errors)"),
        @ApiResponse(responseCode = "404", description = "Referral link or property not found"),
        @ApiResponse(responseCode = "409", description = "Property not available for selected dates (overlapping booking)"),
        @ApiResponse(responseCode = "429", description = "Too many requests - rate limit exceeded (5 per 15 minutes)")
    })
    public ResponseEntity<BookingDto> createBooking(
            @Valid @RequestBody CreateBookingRequest request,
            HttpServletRequest httpRequest) {

        // ✅ SECURITY FIX: Rate limiting to prevent booking spam (5 requests/15 minutes per IP)
        if (!rateLimitService.tryConsumeBookingCreation(httpRequest)) {
            log.warn("Rate limit exceeded for IP: {} - Booking creation denied",
                    httpRequest.getRemoteAddr());
            throw new TooManyRequestsException(
                    "Rate limit exceeded for booking creation. Maximum 5 booking requests per 15 minutes.");
        }

        log.info("Public booking request received: propertyId={}, referralLinkId={}, IP={}",
                request.propertyId(), request.referralLinkId(), httpRequest.getRemoteAddr());

        // Execute use case
        BookingDto created = createBookingUseCase.execute(request);

        log.info("Public booking created successfully: bookingId={}, status={}",
                created.id(), created.status());

        // Return 201 Created with Location header
        return ResponseEntity
                .created(URI.create("/api/public/bookings/" + created.id()))
                .body(created);
    }

    /**
     * Health check endpoint to verify public booking API is accessible.
     *
     * @return simple message confirming API is reachable
     */
    @GetMapping("/health")
    @Operation(
        summary = "Health check for public booking API",
        description = "Verifies that the public booking endpoint is accessible without authentication."
    )
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Public Booking API is operational");
    }
}
