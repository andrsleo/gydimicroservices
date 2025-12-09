package com.affiliate.rentals.gydi.payment.adapters.in.eventhandler;

import com.affiliate.rentals.gydi.bookings.domain.event.BookingFinishedEvent;
import com.affiliate.rentals.gydi.payment.domain.port.in.CreatePaymentUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Event Handler: Listens to BookingFinishedEvent and creates payment.
 * <p>
 * This is the integration point between Bookings and Payment bounded contexts.
 * When a booking is marked as FINISHED, this handler automatically creates
 * a payment record with calculated commission.
 * </p>
 * <p>
 * <strong>IMPORTANT:</strong> This handler runs asynchronously to avoid blocking
 * the booking update operation. If payment creation fails, it will be logged
 * but won't affect the booking status.
 * </p>
 */
@Component
public class BookingFinishedEventHandler {

    private static final Logger log = LoggerFactory.getLogger(BookingFinishedEventHandler.class);

    // Default commission percentages by plan (TODO: fetch from subscription service)
    private static final BigDecimal BASIC_COMMISSION = new BigDecimal("0.02");  // 2%
    private static final BigDecimal PRO_COMMISSION = new BigDecimal("0.05");    // 5%
    private static final BigDecimal PLUS_COMMISSION = new BigDecimal("0.15");   // 15%

    private final CreatePaymentUseCase createPaymentUseCase;

    public BookingFinishedEventHandler(CreatePaymentUseCase createPaymentUseCase) {
        this.createPaymentUseCase = createPaymentUseCase;
    }

    /**
     * Handles BookingFinishedEvent by creating a payment record.
     * <p>
     * Runs asynchronously to avoid blocking the booking update.
     * </p>
     */
    @EventListener
    @Async
    @Transactional
    public void handleBookingFinished(BookingFinishedEvent event) {
        log.info("Received BookingFinishedEvent for booking {}, referral link {}",
            event.bookingId(), event.referralLinkId());

        try {
            // TODO: THIS HANDLER NEEDS TO BE REFACTORED
            // After V48/V50 migrations:
            // - BookingFinishedEvent no longer contains totalAmount/currency
            // - Financial data should come from external source (property pricing API, user input, etc.)
            // - Should create commission_booking record instead of payment record
            // - Need to determine where to get booking amount (property service? stored elsewhere?)

            log.warn("BookingFinishedEvent handler not yet implemented for new architecture. " +
                "Booking {} finished but no commission record created. " +
                "TODO: Implement commission_booking creation with external amount source.",
                event.bookingId());

            // Temporarily disabled - needs refactoring to work with new schema
            /*
            if (event.userId() == null) {
                log.error("Cannot create payment: userId is null for booking {}. " +
                    "Need to fetch from referral_link table.", event.bookingId());
                return;
            }

            BigDecimal commissionPercentage = PRO_COMMISSION;

            CreatePaymentUseCase.CreatePaymentCommand command = new CreatePaymentUseCase.CreatePaymentCommand(
                event.bookingId(),
                event.referralLinkId(),
                event.userId(),
                ??? // totalAmount - where does this come from now?
                ??? // currency - where does this come from now?
                commissionPercentage
            );

            CreatePaymentUseCase.PaymentResponse payment = createPaymentUseCase.execute(command);

            log.info("Payment created successfully with ID {} for booking {}, commission: {}",
                payment.id(), event.bookingId(), payment.commissionAmount());
            */

        } catch (Exception e) {
            log.error("Failed to create payment for booking {}: {}",
                event.bookingId(), e.getMessage(), e);
            // Don't rethrow - we don't want to affect the booking update
            // TODO: Implement retry mechanism or dead letter queue
        }
    }
}
