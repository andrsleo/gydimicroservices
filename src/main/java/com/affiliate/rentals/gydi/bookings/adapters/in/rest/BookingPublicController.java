package com.affiliate.rentals.gydi.bookings.adapters.in.rest;

import com.affiliate.rentals.gydi.bookings.application.dto.CreateBookingRequest;
import com.affiliate.rentals.gydi.bookings.domain.port.in.CreateBookingUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public REST Controller for booking submissions.
 * <p>
 * This endpoint is publicly accessible (no authentication) for clients
 * to submit booking requests via referral links.
 * </p>
 */
@RestController
@RequestMapping("/api/public/bookings")
@Tag(name = "Bookings Public", description = "Public booking submission endpoints")
public class BookingPublicController {

    private static final Logger log = LoggerFactory.getLogger(BookingPublicController.class);

    private final CreateBookingUseCase createBookingUseCase;

    public BookingPublicController(CreateBookingUseCase createBookingUseCase) {
        this.createBookingUseCase = createBookingUseCase;
    }

    @PostMapping
    @Operation(summary = "Submit booking request", description = "Creates a new booking request via referral link. " +
            "No authentication required - publicly accessible endpoint.")
    public ResponseEntity<CreateBookingUseCase.BookingResponse> createBooking(
            @Valid @RequestBody CreateBookingRequest request) {
        log.info("POST /api/public/bookings - Creating booking for property {} via referral link {}",
                request.propertyId(), request.referralLinkId());

        CreateBookingUseCase.CreateBookingCommand command = new CreateBookingUseCase.CreateBookingCommand(
                request.referralLinkId(), // Pass as String
                request.propertyId(),
                request.startDate(),
                request.endDate(),
                request.clientEmail(),
                request.clientPhone(),
                request.clientFirstName(),
                request.clientLastName());

        CreateBookingUseCase.BookingResponse response = createBookingUseCase.execute(command);

        log.info("Booking created successfully with ID: {}", response.bookingId());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/check-availability")
    @Operation(summary = "Check property availability", description = "Checks if a property is available for the specified date range. "
            +
            "Returns 200 OK if available, 409 CONFLICT if not available.")
    public ResponseEntity<Void> checkAvailability(
            @RequestParam Long propertyId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        log.info("GET /api/public/bookings/check-availability - Property: {}, Dates: {} to {}",
                propertyId, startDate, endDate);

        // TODO: Implement availability check use case
        // For now, return 200 OK (available)
        return ResponseEntity.ok().build();
    }
}
