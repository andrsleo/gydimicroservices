package com.affiliate.rentals.gydi.commissions.application.event;

import com.affiliate.rentals.gydi.commissions.application.usecase.ChargeHostCommissionUseCase;
import com.affiliate.rentals.gydi.commissions.application.usecase.CreateCommissionsFromBookingUseCase;
import com.affiliate.rentals.gydi.commissions.domain.events.BookingFinishedEvent;
import com.affiliate.rentals.gydi.commissions.domain.model.HostCommission;
import com.affiliate.rentals.gydi.commissions.domain.ports.HostCommissionRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Event Handler: Reacts to BookingFinishedEvent to automatically charge host commission.
 * <p>
 * Business Flow:
 * 1. Booking transitions to FINISHED status
 * 2. BookingFinishedEvent is published (AFTER_COMMIT)
 * 3. CreateCommissionsFromBookingUseCase creates HostCommission + AffiliateCommission records (PENDING)
 * 4. ChargeHostCommissionUseCase immediately charges host via Stripe Payment Intent
 * </p>
 * <p>
 * Error Handling:
 * - Uses AFTER_COMMIT phase to ensure booking is persisted
 * - Catches exceptions to prevent transaction rollback
 * - Logs errors for monitoring/alerting
 * </p>
 */
@Component("commissionBookingFinishedEventHandler")
@RequiredArgsConstructor
public class BookingFinishedEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(BookingFinishedEventHandler.class);

    private final CreateCommissionsFromBookingUseCase createCommissionsUseCase;
    private final ChargeHostCommissionUseCase chargeHostCommissionUseCase;
    private final HostCommissionRepositoryPort hostCommissionRepository;

    /**
     * Handles BookingFinishedEvent: creates commissions and immediately charges host.
     *
     * @param event the booking finished event
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBookingFinished(BookingFinishedEvent event) {
        logger.info("========================================");
        logger.info("Received BookingFinishedEvent for booking ID: {}", event.bookingId());
        logger.info("========================================");

        try {
            // Step 1: Create commission records (HostCommission + AffiliateCommission)
            logger.info("Step 1/2: Creating commission records for booking {}", event.bookingId());
            createCommissionsUseCase.execute(event);
            logger.info("Commission records created successfully");

            // Step 2: Immediately charge host commission via Stripe
            logger.info("Step 2/2: Charging host commission for booking {}", event.bookingId());

            // Find the HostCommission that was just created
            HostCommission hostCommission = hostCommissionRepository.findByBookingId(event.bookingId())
                    .orElseThrow(() -> new IllegalStateException(
                            "HostCommission not found for booking " + event.bookingId()));

            // Execute charge
            chargeHostCommissionUseCase.execute(hostCommission.getId());

            logger.info("========================================");
            logger.info("Booking {} processing complete", event.bookingId());
            logger.info("========================================");

        } catch (Exception e) {
            // IMPORTANT: Do NOT re-throw exception
            // We don't want to fail the transaction if commission processing fails
            // Commission retry logic will handle failed charges
            logger.error("========================================");
            logger.error("Failed to process commissions for booking ID: {}", event.bookingId(), e);
            logger.error("Reason: {}", e.getMessage());
            logger.error("This will be retried by the scheduler if charge failed");
            logger.error("========================================");

            // TODO: Send alert to monitoring system (e.g., Slack, PagerDuty)
            // TODO: Implement dead letter queue for failed events
        }
    }
}
