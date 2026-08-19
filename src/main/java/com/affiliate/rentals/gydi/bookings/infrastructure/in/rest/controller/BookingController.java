package com.affiliate.rentals.gydi.bookings.infrastructure.in.rest.controller;

import com.affiliate.rentals.gydi.bookings.application.dto.*;
import com.affiliate.rentals.gydi.bookings.application.usecase.*;
import com.affiliate.rentals.gydi.bookings.application.usecase.CreateBookingPaymentUseCase.CreatePaymentCommand;
import com.affiliate.rentals.gydi.bookings.application.usecase.CaptureDamageDepositUseCase.CaptureCommand;
import com.affiliate.rentals.gydi.shared.exception.TooManyRequestsException;
import com.affiliate.rentals.gydi.shared.security.CustomUserDetails;
import com.affiliate.rentals.gydi.shared.security.RateLimitService;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/**
 * REST Controller for Booking operations.
 * <p>
 * Provides endpoints for ADMIN to manage bookings.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/bookings")
@Tag(name = "Bookings", description = "Booking management endpoints for ADMIN")
public class BookingController {

    private final CreateBookingUseCase createBookingUseCase;
    private final ReserveBookingUseCase reserveBookingUseCase;
    private final CancelBookingUseCase cancelBookingUseCase;
    private final StartBookingUseCase startBookingUseCase;
    private final FinishBookingUseCase finishBookingUseCase;
    private final DisputeBookingUseCase disputeBookingUseCase;
    private final GetAllBookingsUseCase getAllBookingsUseCase;
    private final GetBookingByIdUseCase getBookingByIdUseCase;
    private final GetAffiliateBookingsUseCase getAffiliateBookingsUseCase;
    private final GetAffiliateBookingStatsUseCase getAffiliateBookingStatsUseCase;
    private final GetHostBookingsUseCase getHostBookingsUseCase;
    private final GetHostBookingStatsUseCase getHostBookingStatsUseCase;
    private final RejectBookingUseCase rejectBookingUseCase;
    private final CreateBookingPaymentUseCase createBookingPaymentUseCase;
    private final CaptureDamageDepositUseCase captureDamageDepositUseCase;
    private final RateLimitService rateLimitService;

    public BookingController(
            CreateBookingUseCase createBookingUseCase,
            ReserveBookingUseCase reserveBookingUseCase,
            CancelBookingUseCase cancelBookingUseCase,
            StartBookingUseCase startBookingUseCase,
            FinishBookingUseCase finishBookingUseCase,
            DisputeBookingUseCase disputeBookingUseCase,
            GetAllBookingsUseCase getAllBookingsUseCase,
            GetBookingByIdUseCase getBookingByIdUseCase,
            GetAffiliateBookingsUseCase getAffiliateBookingsUseCase,
            GetAffiliateBookingStatsUseCase getAffiliateBookingStatsUseCase,
            GetHostBookingsUseCase getHostBookingsUseCase,
            GetHostBookingStatsUseCase getHostBookingStatsUseCase,
            RejectBookingUseCase rejectBookingUseCase,
            CreateBookingPaymentUseCase createBookingPaymentUseCase,
            CaptureDamageDepositUseCase captureDamageDepositUseCase,
            RateLimitService rateLimitService) {
        this.createBookingUseCase = createBookingUseCase;
        this.reserveBookingUseCase = reserveBookingUseCase;
        this.cancelBookingUseCase = cancelBookingUseCase;
        this.startBookingUseCase = startBookingUseCase;
        this.finishBookingUseCase = finishBookingUseCase;
        this.disputeBookingUseCase = disputeBookingUseCase;
        this.getAllBookingsUseCase = getAllBookingsUseCase;
        this.getBookingByIdUseCase = getBookingByIdUseCase;
        this.getAffiliateBookingsUseCase = getAffiliateBookingsUseCase;
        this.getAffiliateBookingStatsUseCase = getAffiliateBookingStatsUseCase;
        this.getHostBookingsUseCase = getHostBookingsUseCase;
        this.getHostBookingStatsUseCase = getHostBookingStatsUseCase;
        this.rejectBookingUseCase = rejectBookingUseCase;
        this.createBookingPaymentUseCase = createBookingPaymentUseCase;
        this.captureDamageDepositUseCase = captureDamageDepositUseCase;
        this.rateLimitService = rateLimitService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all bookings (ADMIN only)", description = "Returns all bookings in the system for administrative management")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bookings retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Not authorized - ADMIN role required")
    })
    public ResponseEntity<List<BookingDto>> getAllBookings() {
        List<BookingDto> bookings = getAllBookingsUseCase.execute();
        return ResponseEntity.ok(bookings);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new booking request", description = "Rate limited to 5 requests per 15 minutes per IP")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Booking created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "404", description = "Referral link or property not found"),
            @ApiResponse(responseCode = "429", description = "Too many requests - rate limit exceeded (5 per 15 minutes)")
    })
    public ResponseEntity<BookingDto> createBooking(
            @Valid @RequestBody CreateBookingRequest request,
            HttpServletRequest httpRequest) {
        // ✅ SECURITY FIX: Rate limiting to prevent booking spam (5 requests/15 minutes
        // per IP)
        if (!rateLimitService.tryConsumeBookingCreation(httpRequest)) {
            throw new TooManyRequestsException(
                    "Rate limit exceeded for booking creation. Maximum 5 booking requests per 15 minutes.");
        }

        BookingDto created = createBookingUseCase.execute(request);
        return ResponseEntity
                .created(URI.create("/api/v1/bookings/" + created.id()))
                .body(created);
    }

    @PutMapping("/{bookingId}/reserve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reserve a booking (ADMIN only)", description = "Confirms booking in Airbnb and transitions to RESERVED status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking reserved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid status transition"),
            @ApiResponse(responseCode = "403", description = "Not authorized"),
            @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    public ResponseEntity<BookingDto> reserveBooking(
            @PathVariable Long bookingId,
            @Valid @RequestBody ReserveBookingRequest request) {
        BookingDto reserved = reserveBookingUseCase.execute(bookingId, request);
        return ResponseEntity.ok(reserved);
    }

    @PutMapping("/{bookingId}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cancel a booking (ADMIN only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking cancelled successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid status transition"),
            @ApiResponse(responseCode = "403", description = "Not authorized"),
            @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    public ResponseEntity<BookingDto> cancelBooking(
            @PathVariable Long bookingId,
            @Valid @RequestBody CancelBookingRequest request) {
        BookingDto cancelled = cancelBookingUseCase.execute(bookingId, request);
        return ResponseEntity.ok(cancelled);
    }

    @PutMapping("/{bookingId}/start")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Start a booking / manual check-in (ADMIN only)",
            description = "Manually transitions a booking from RESERVED to IN_PROGRESS. "
                    + "Normally this is automated by the scheduler on the check-in date. "
                    + "Use this endpoint to override when the scheduler has not yet run.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking started successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid status transition (booking is not RESERVED)"),
            @ApiResponse(responseCode = "403", description = "Not authorized - ADMIN role required"),
            @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    public ResponseEntity<BookingDto> startBooking(@PathVariable Long bookingId) {
        BookingDto started = startBookingUseCase.execute(bookingId);
        return ResponseEntity.ok(started);
    }

    @PutMapping("/{bookingId}/finish")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Finish a booking / manual check-out (ADMIN only)",
            description = "Manually transitions a booking from IN_PROGRESS to FINISHED. "
                    + "Triggers commission creation and host charge via BookingFinishedEvent. "
                    + "Normally automated by the scheduler on the check-out date.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking finished successfully and commissions triggered"),
            @ApiResponse(responseCode = "400", description = "Invalid status transition (booking is not IN_PROGRESS)"),
            @ApiResponse(responseCode = "403", description = "Not authorized - ADMIN role required"),
            @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    public ResponseEntity<BookingDto> finishBooking(@PathVariable Long bookingId) {
        BookingDto finished = finishBookingUseCase.execute(bookingId);
        return ResponseEntity.ok(finished);
    }

    @PutMapping("/{bookingId}/dispute")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Dispute a finished booking (ADMIN only)",
            description = "Marks a FINISHED booking as DISPUTED when there is a complaint or issue. "
                    + "Allows for commission adjustment or refund processing.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking marked as disputed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid status transition (booking is not FINISHED)"),
            @ApiResponse(responseCode = "403", description = "Not authorized - ADMIN role required"),
            @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    public ResponseEntity<BookingDto> disputeBooking(
            @PathVariable Long bookingId,
            @Valid @RequestBody DisputeBookingRequest request) {
        BookingDto disputed = disputeBookingUseCase.execute(bookingId, request);
        return ResponseEntity.ok(disputed);
    }

    // ========================================
    // AFFILIATE ENDPOINTS (must come BEFORE /{bookingId})
    // ========================================
    // TODO: Implement proper role assignment based on subscription plans
    // Currently allowing ROLE_USER temporarily to unblock frontend
    // Proper implementation should assign ROLE_AFFILIATE when user subscribes to a plan

    @GetMapping("/affiliate")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get bookings for affiliate", description = "Returns all bookings generated through affiliate's referral links")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bookings retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Not authorized")
    })
    public ResponseEntity<List<BookingDto>> getAffiliateBookings(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long affiliateId = userDetails.getUserId();
        List<BookingDto> bookings = getAffiliateBookingsUseCase.execute(affiliateId);
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/affiliate/stats")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get booking statistics for affiliate", description = "Returns statistics for bookings generated through affiliate's referral links")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stats retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Not authorized")
    })
    public ResponseEntity<BookingStatsDto> getAffiliateBookingStats(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long affiliateId = userDetails.getUserId();
        BookingStatsDto stats = getAffiliateBookingStatsUseCase.execute(affiliateId);
        return ResponseEntity.ok(stats);
    }

    // ========================================
    // HOST ENDPOINTS (must come BEFORE /{bookingId})
    // ========================================

    @GetMapping("/host")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get bookings for host", description = "Returns all bookings for properties owned by the host")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bookings retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Not authorized")
    })
    public ResponseEntity<List<BookingDto>> getHostBookings(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long hostId = userDetails.getUserId();
        List<BookingDto> bookings = getHostBookingsUseCase.execute(hostId);
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/host/stats")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get booking statistics for host", description = "Returns statistics for bookings on properties owned by the host")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stats retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Not authorized")
    })
    public ResponseEntity<BookingStatsDto> getHostBookingStats(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long hostId = userDetails.getUserId();
        BookingStatsDto stats = getHostBookingStatsUseCase.execute(hostId);
        return ResponseEntity.ok(stats);
    }

    // ========================================
    // GENERIC ENDPOINT (must come AFTER specific routes)
    // ========================================

    @GetMapping("/{bookingId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get booking by ID", description = "Returns booking if user owns it (as affiliate or host) or is ADMIN")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking found"),
            @ApiResponse(responseCode = "403", description = "Not authorized or don't own this booking"),
            @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    public ResponseEntity<BookingDto> getBookingById(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long requestingUserId = userDetails.getUserId();
        boolean isAdmin = userDetails.hasRole("ADMIN");

        BookingDto booking = getBookingByIdUseCase.execute(bookingId, requestingUserId, isAdmin);
        return ResponseEntity.ok(booking);
    }

    // ========================================
    // HOST CONFIRM / REJECT ENDPOINTS
    // ========================================

    @PutMapping("/{id}/confirm")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "HOST confirms a booking request (REQUEST → RESERVED)",
               description = "The property host confirms the guest booking directly. Blocks dates in property calendar.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking confirmed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid status transition"),
            @ApiResponse(responseCode = "403", description = "Not authorized — HOST role required or not property owner"),
            @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    public ResponseEntity<BookingDto> confirmBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long hostId = userDetails.getUserId();
        BookingDto result = reserveBookingUseCase.executeByHost(id, hostId);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "HOST rejects a booking request (REQUEST → CANCELLED)",
               description = "The property host rejects the guest booking request with a reason.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking rejected successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or status transition"),
            @ApiResponse(responseCode = "403", description = "Not authorized — HOST role required or not property owner"),
            @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    public ResponseEntity<BookingDto> rejectBooking(
            @PathVariable Long id,
            @RequestBody @Valid RejectBookingRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long hostId = userDetails.getUserId();
        BookingDto result = rejectBookingUseCase.execute(id, request.reason(), hostId);
        return ResponseEntity.ok(result);
    }

    // ========================================
    // PHASE 2: PAYMENT ENDPOINTS
    // ========================================

    @PostMapping("/{id}/payment")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Initiate booking payment + security deposit hold (Phase 2)",
               description = "Guest initiates payment after booking is confirmed. Creates Stripe PaymentIntent and deposit hold.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Payment initiated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "403", description = "Not authorized"),
            @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    public ResponseEntity<Void> createPayment(
            @PathVariable Long id,
            @RequestBody @Valid CreateBookingPaymentRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        createBookingPaymentUseCase.execute(
                new CreatePaymentCommand(id, request.stripePaymentMethodId(), userDetails.getUserId())
        );
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/deposit/capture")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Capture security deposit for damage (Phase 2, HOST only)",
               description = "Host captures the security deposit after check-out when damage is reported. 7-day window.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deposit captured successfully"),
            @ApiResponse(responseCode = "400", description = "Capture window expired or invalid state"),
            @ApiResponse(responseCode = "403", description = "Not authorized — HOST role required"),
            @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    public ResponseEntity<Void> captureDamageDeposit(
            @PathVariable Long id,
            @RequestBody @Valid CaptureDamageDepositRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        captureDamageDepositUseCase.execute(
                new CaptureCommand(id, request.damageAmountCents(), request.description(), userDetails.getUserId())
        );
        return ResponseEntity.noContent().build();
    }

    // TODO: Implement additional endpoints
    // - GET /api/v1/bookings/property/{propertyId}
    // - GET /api/v1/bookings/{id}/timeline (status history)
}
