package com.affiliate.rentals.gydi.commissions.application.usecase;

import com.affiliate.rentals.gydi.commissions.domain.events.BookingFinishedEvent;
import com.affiliate.rentals.gydi.commissions.domain.model.ReferralCommission;
import com.affiliate.rentals.gydi.commissions.domain.model.HostCommission;
import com.affiliate.rentals.gydi.commissions.domain.ports.ReferralCommissionRepositoryPort;
import com.affiliate.rentals.gydi.commissions.domain.ports.HostCommissionRepositoryPort;
import com.affiliate.rentals.gydi.commissions.domain.service.CommissionCalculationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use Case: Creates commission records when a booking is finished.
 * <p>
 * This is the main entry point triggered by {@link BookingFinishedEvent}.
 * Creates both:
 * - Host commission (platform CHARGES host)
 * - Affiliate commission (platform PAYS affiliate) - only if booking has
 * affiliate
 * </p>
 */
@Service
public class CreateCommissionsFromBookingUseCase {

    private static final Logger logger = LoggerFactory.getLogger(CreateCommissionsFromBookingUseCase.class);

    private final CommissionCalculationService calculationService;
    private final HostCommissionRepositoryPort hostCommissionRepository;
    private final ReferralCommissionRepositoryPort affiliateCommissionRepository;

    public CreateCommissionsFromBookingUseCase(
            CommissionCalculationService calculationService,
            HostCommissionRepositoryPort hostCommissionRepository,
            ReferralCommissionRepositoryPort affiliateCommissionRepository) {
        this.calculationService = calculationService;
        this.hostCommissionRepository = hostCommissionRepository;
        this.affiliateCommissionRepository = affiliateCommissionRepository;
    }

    /**
     * Executes commission creation from booking finished event.
     *
     * @param event booking finished event
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void execute(BookingFinishedEvent event) {
        logger.info("Creating commissions for booking ID: {}", event.bookingId());

        // Idempotency check: prevent duplicate commission creation
        if (hostCommissionRepository.existsByBookingId(event.bookingId())) {
            logger.warn("Host commission already exists for booking ID: {}, skipping", event.bookingId());
            return;
        }

        // 1. Create and save HOST commission (platform charges host)
        HostCommission hostCommission = calculationService.calculateHostCommissionWithRate(
                event.bookingId(),
                event.hostId(),
                event.totalAmount(),
                event.currency(),
                event.hostCommissionRate());
        HostCommission savedHostCommission = hostCommissionRepository.save(hostCommission);
        logger.info("Created host commission ID: {} for booking ID: {}, amount: {} {}",
                savedHostCommission.getId(),
                event.bookingId(),
                savedHostCommission.getAmount().getCommissionAmount(),
                savedHostCommission.getAmount().getCurrency());

        // 2. Create and save AFFILIATE commission (platform pays affiliate) - only if
        // booking has affiliate
        if (event.hasAffiliate()) {
            ReferralCommission affiliateCommission = calculationService.calculateAffiliateCommissionWithRate(
                    event.bookingId(),
                    event.affiliateId(),
                    event.totalAmount(),
                    event.currency(),
                    event.finishedAt(),
                    event.affiliateCommissionRate());
            ReferralCommission savedAffiliateCommission = affiliateCommissionRepository.save(affiliateCommission);
            logger.info(
                    "Created affiliate commission ID: {} for booking ID: {}, amount: {} {}, status: WAITING_HOST_CHARGE (will be approved after host is charged), payment scheduled for: {}",
                    savedAffiliateCommission.getId(),
                    event.bookingId(),
                    savedAffiliateCommission.getAmount().getCommissionAmount(),
                    savedAffiliateCommission.getAmount().getCurrency(),
                    savedAffiliateCommission.getPaymentSchedule().getScheduledPaymentDate());
        } else {
            // Organic booking (affiliate commission rate is 0 or no real affiliate):
            // no ReferralCommission record is created; 100% of host fee goes to platform.
            boolean isOrganic = event.affiliateId() != null
                    && (event.affiliateCommissionRate() == null
                        || event.affiliateCommissionRate().compareTo(java.math.BigDecimal.ZERO) == 0);
            if (isOrganic) {
                logger.info("Booking ID: {} is an organic booking (affiliate commission rate = 0). "
                        + "Skipping affiliate commission creation; host fee retained by platform.", event.bookingId());
            } else {
                logger.debug("Booking ID: {} has no affiliate, skipping affiliate commission", event.bookingId());
            }
        }
    }
}
