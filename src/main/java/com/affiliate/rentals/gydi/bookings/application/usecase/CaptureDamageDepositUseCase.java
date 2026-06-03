package com.affiliate.rentals.gydi.bookings.application.usecase;

import com.affiliate.rentals.gydi.bookings.domain.exception.BookingAccessDeniedException;
import com.affiliate.rentals.gydi.bookings.domain.exception.BookingNotFoundException;
import com.affiliate.rentals.gydi.bookings.domain.exception.InvalidBookingStateException;
import com.affiliate.rentals.gydi.bookings.domain.model.Booking;
import com.affiliate.rentals.gydi.bookings.domain.ports.BookingPaymentPort;
import com.affiliate.rentals.gydi.bookings.domain.ports.BookingRepositoryPort;
import com.affiliate.rentals.gydi.properties.domain.exception.PropertyNotFoundException;
import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.model.PropertyId;
import com.affiliate.rentals.gydi.properties.domain.ports.out.PropertyRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Use Case: Capture security deposit for damage (HOST only).
 * <p>
 * Phase 2 — allows the host to capture the security deposit hold after check-out
 * when damage has been reported. Must be executed within 7 days of check-out.
 * </p>
 * <p>
 * Business Flow:
 * 1. Validate host owns the property
 * 2. Validate booking is in terminal state (FINISHED or DISPUTED)
 * 3. Validate 7-day capture window not expired
 * 4. Capture deposit via Stripe
 * 5. Record capture on the booking domain model
 * </p>
 */
@Service
public class CaptureDamageDepositUseCase {

    private static final Logger log = LoggerFactory.getLogger(CaptureDamageDepositUseCase.class);

    private final BookingRepositoryPort bookingRepository;
    private final BookingPaymentPort paymentPort;
    private final PropertyRepositoryPort propertyRepository;

    public record CaptureCommand(
            Long bookingId,
            Long damageAmountCents,    // null = capture full deposit
            String damageDescription,
            Long requestingHostId
    ) {}

    public CaptureDamageDepositUseCase(BookingRepositoryPort bookingRepository,
                                       BookingPaymentPort paymentPort,
                                       PropertyRepositoryPort propertyRepository) {
        this.bookingRepository = Objects.requireNonNull(bookingRepository);
        this.paymentPort = Objects.requireNonNull(paymentPort);
        this.propertyRepository = Objects.requireNonNull(propertyRepository);
    }

    @Transactional
    public void execute(CaptureCommand command) {
        log.info("HOST {} capturing damage deposit for booking {}", command.requestingHostId(), command.bookingId());

        Booking booking = bookingRepository.findById(command.bookingId())
                .orElseThrow(() -> new BookingNotFoundException(command.bookingId()));

        // Only the host of the property can capture
        Property property = propertyRepository.findById(PropertyId.of(booking.getPropertyId()))
                .orElseThrow(() -> new PropertyNotFoundException(String.valueOf(booking.getPropertyId())));

        if (!property.getHostId().equals(command.requestingHostId())) {
            throw new BookingAccessDeniedException("Only property host can capture security deposit");
        }

        // Booking must be in a terminal state (FINISHED or DISPUTED)
        if (!booking.getStatus().isTerminal()) {
            throw new InvalidBookingStateException(
                    "Cannot capture deposit for booking in status " + booking.getStatus());
        }

        // Validate 7-day capture window after check-out
        if (booking.getCheckOutDate().plusDays(7).isBefore(LocalDate.now())) {
            throw new InvalidBookingStateException(
                    "Deposit capture window (7 days after check-out) has expired");
        }

        // Capture via Stripe
        paymentPort.captureSecurityDeposit(
                command.bookingId(),
                booking.getStripeDepositIntentId(),
                command.damageAmountCents()
        );

        // Record capture on domain model
        booking.recordDepositCapture(command.damageAmountCents(), command.damageDescription());
        bookingRepository.save(booking);

        log.info("Deposit captured for booking {} — amount={}",
                command.bookingId(), command.damageAmountCents() != null ? command.damageAmountCents() : "full");
    }
}
