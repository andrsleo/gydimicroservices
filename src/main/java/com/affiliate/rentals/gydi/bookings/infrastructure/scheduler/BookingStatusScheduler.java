package com.affiliate.rentals.gydi.bookings.infrastructure.scheduler;

import com.affiliate.rentals.gydi.bookings.domain.model.Booking;
import com.affiliate.rentals.gydi.bookings.domain.ports.BookingRepositoryPort;
import com.affiliate.rentals.gydi.commissions.domain.events.BookingFinishedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Scheduler to automate booking status transitions.
 * <p>
 * 1. RESERVED -> IN_PROGRESS (Check-in)
 * 2. IN_PROGRESS -> FINISHED (Check-out)
 * </p>
 */
@Component
public class BookingStatusScheduler {

    private static final Logger log = LoggerFactory.getLogger(BookingStatusScheduler.class);

    private final BookingRepositoryPort bookingRepository;
    private final ApplicationEventPublisher eventPublisher;

    public BookingStatusScheduler(BookingRepositoryPort bookingRepository, ApplicationEventPublisher eventPublisher) {
        this.bookingRepository = bookingRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Runs daily at midnight (or periodically) to update booking statuses.
     * Cron: "0 0 12 * * *" (At 12:00 PM every day) - ADJUST AS NEEDED
     * For testing/demo, running every hour might be better: "0 0 * * * *"
     */
    @Scheduled(cron = "0 0 12 * * *") // Daily at 12 PM
    @Transactional
    public void updateBookingStatuses() {
        log.info("Starting scheduled booking status updates...");
        LocalDate today = LocalDate.now();

        processCheckIns(today);
        processCheckOuts(today);

        log.info("Completed scheduled booking status updates.");
    }

    private void processCheckIns(LocalDate today) {
        List<Booking> toStart = bookingRepository.findReadyToStart(today);
        log.info("Found {} bookings ready to start (Check-in <= {})", toStart.size(), today);

        for (Booking booking : toStart) {
            try {
                booking.startStay();
                bookingRepository.save(booking);
                log.info("Booking {} started successfully.", booking.getId());
            } catch (Exception e) {
                log.error("Failed to start booking {}: {}", booking.getId(), e.getMessage());
            }
        }
    }

    private void processCheckOuts(LocalDate today) {
        List<Booking> toFinish = bookingRepository.findReadyToFinish(today);
        log.info("Found {} bookings ready to finish (Check-out <= {})", toFinish.size(), today);

        for (Booking booking : toFinish) {
            try {
                // 1. Finish the booking (updates status)
                booking.finish();
                bookingRepository.save(booking);

                // 2. Publish event to trigger commission creation
                // Note: We need to fetch hostId again if it's not in the booking object.
                // Or we can rely on the use case that handles the event to fetch it?
                // The event requires hostId. Booking doesn't have it.
                // We need to fetch it here.

                Long hostId = bookingRepository.findHostIdByPropertyId(booking.getPropertyId())
                        .orElseThrow(() -> new IllegalStateException(
                                "Host not found for property " + booking.getPropertyId()));

                Long affiliateId = null;
                if (booking.getReferralLinkId() != null) {
                    affiliateId = bookingRepository.findAffiliateIdByReferralLinkId(booking.getReferralLinkId())
                            .orElse(null);
                }

                BookingFinishedEvent event = new BookingFinishedEvent(
                        booking.getId(),
                        booking.getPropertyId(),
                        hostId,
                        affiliateId,
                        booking.getTotalAmount().getAmount(),
                        booking.getTotalAmount().getCurrency(),
                        booking.getHostCommissionRate(), // Using SNAPSHOTTED rate
                        booking.getAffiliateCommissionRate(), // Using SNAPSHOTTED rate
                        booking.getFinishedAt());

                eventPublisher.publishEvent(event);

                log.info("Booking {} finished successfully. Event published.", booking.getId());

            } catch (Exception e) {
                log.error("Failed to finish booking {}: {}", booking.getId(), e.getMessage());
            }
        }
    }
}
