package com.affiliate.rentals.gydi.bookings.adapters.in.rest;

import com.affiliate.rentals.gydi.bookings.application.dto.BookingResponse;
import com.affiliate.rentals.gydi.bookings.application.dto.UpdateBookingStatusRequest;
import com.affiliate.rentals.gydi.bookings.domain.port.in.CreateBookingUseCase;
import com.affiliate.rentals.gydi.bookings.domain.port.in.GetBookingUseCase;
import com.affiliate.rentals.gydi.bookings.domain.port.in.UpdateBookingStatusUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for booking management (Admin/Host operations).
 * <p>
 * Endpoints protected by Spring Security - require ADMIN or HOST role.
 * </p>
 */
@RestController
@RequestMapping("/api/admin/bookings")
@Tag(name = "Bookings Management", description = "Admin and Host booking management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class BookingAdminController {

    private static final Logger log = LoggerFactory.getLogger(BookingAdminController.class);

    private final GetBookingUseCase getBookingUseCase;
    private final UpdateBookingStatusUseCase updateBookingStatusUseCase;

    public BookingAdminController(GetBookingUseCase getBookingUseCase,
                                   UpdateBookingStatusUseCase updateBookingStatusUseCase) {
        this.getBookingUseCase = getBookingUseCase;
        this.updateBookingStatusUseCase = updateBookingStatusUseCase;
    }

    @GetMapping("/{bookingId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOST')")
    @Operation(summary = "Get booking by ID", description = "Retrieves booking details by ID (ADMIN/HOST only)")
    public ResponseEntity<CreateBookingUseCase.BookingResponse> getBookingById(
        @PathVariable Long bookingId
    ) {
        log.info("GET /api/admin/bookings/{} - Get booking by ID", bookingId);

        CreateBookingUseCase.BookingResponse response = getBookingUseCase.getById(bookingId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/referral-link/{referralLinkId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOST')")
    @Operation(
        summary = "Get bookings by referral link",
        description = "Retrieves all bookings for a specific referral link (ADMIN/HOST only)"
    )
    public ResponseEntity<List<CreateBookingUseCase.BookingResponse>> getBookingsByReferralLink(
        @PathVariable Long referralLinkId
    ) {
        log.info("GET /api/admin/bookings/referral-link/{} - Get bookings by referral link", referralLinkId);

        List<CreateBookingUseCase.BookingResponse> bookings =
            getBookingUseCase.getByReferralLinkId(referralLinkId);

        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/property/{propertyId}/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOST')")
    @Operation(
        summary = "Get active bookings for property",
        description = "Retrieves all active bookings (REQUEST or RESERVED) for a property (ADMIN/HOST only)"
    )
    public ResponseEntity<List<CreateBookingUseCase.BookingResponse>> getActiveBookingsByProperty(
        @PathVariable Long propertyId
    ) {
        log.info("GET /api/admin/bookings/property/{}/active - Get active bookings", propertyId);

        List<CreateBookingUseCase.BookingResponse> bookings =
            getBookingUseCase.getActiveBookingsByProperty(propertyId);

        return ResponseEntity.ok(bookings);
    }

    @PatchMapping("/{bookingId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOST')")
    @Operation(
        summary = "Update booking status",
        description = "Updates booking status: REQUEST→RESERVED, RESERVED→FINISHED, or any→CANCELED (ADMIN/HOST only)"
    )
    public ResponseEntity<CreateBookingUseCase.BookingResponse> updateBookingStatus(
        @PathVariable Long bookingId,
        @Valid @RequestBody UpdateBookingStatusRequest request
    ) {
        log.info("PATCH /api/admin/bookings/{}/status - Update to status: {}",
            bookingId, request.targetStatus());

        UpdateBookingStatusUseCase.UpdateStatusCommand command =
            new UpdateBookingStatusUseCase.UpdateStatusCommand(
                bookingId,
                request.targetStatus()
            );

        CreateBookingUseCase.BookingResponse response = updateBookingStatusUseCase.execute(command);

        return ResponseEntity.ok(response);
    }
}
