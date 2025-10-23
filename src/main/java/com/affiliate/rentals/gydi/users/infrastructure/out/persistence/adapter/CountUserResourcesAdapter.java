package com.affiliate.rentals.gydi.users.infrastructure.out.persistence.adapter;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.affiliate.rentals.gydi.users.domain.model.ResourceType;
import com.affiliate.rentals.gydi.users.domain.ports.CountUserResourcesPort;

/**
 * Adapter implementation of the CountUserResourcesPort using JPA.
 *
 * <p>This adapter implements the hexagonal architecture's port interface,
 * providing resource counting capabilities for permission evaluation.
 * It queries the properties and referrals tables to count resources
 * owned by users.</p>
 *
 * <p><strong>Note:</strong> This implementation currently provides stub
 * functionality. Full integration with properties and referrals bounded
 * contexts should be implemented when those modules are available.</p>
 *
 * @author GYDI Development Team
 * @see CountUserResourcesPort
 */
@Component
public class CountUserResourcesAdapter implements CountUserResourcesPort {

    // TODO: Inject PropertyJpaRepository when properties bounded context is implemented
    // private final PropertyJpaRepository propertyRepository;

    // TODO: Inject ReferralJpaRepository when referrals bounded context is implemented
    // private final ReferralJpaRepository referralRepository;

    /**
     * Constructs a new CountUserResourcesAdapter.
     *
     * <p>Future dependencies to be injected:</p>
     * <ul>
     *   <li>PropertyJpaRepository - for counting published properties</li>
     *   <li>ReferralJpaRepository - for counting generated referrals</li>
     * </ul>
     */
    public CountUserResourcesAdapter() {
        // Constructor for future dependency injection
    }

    @Override
    @Transactional(readOnly = true)
    public int countUserResources(Long userId, ResourceType resourceType) {
        return switch (resourceType) {
            case PROPERTY -> countUserProperties(userId);
            case REFERRAL -> countUserReferrals(userId);
        };
    }

    @Override
    @Transactional(readOnly = true)
    public int countUserProperties(Long userId) {
        // TODO: Implement when properties bounded context is available
        // return (int) propertyRepository.countByOwnerId(userId);

        // Temporary stub - returns 0 to avoid blocking permission checks
        return 0;
    }

    @Override
    @Transactional(readOnly = true)
    public int countUserReferrals(Long userId) {
        // TODO: Implement when referrals bounded context is available
        // return (int) referralRepository.countByUserId(userId);

        // Temporary stub - returns 0 to avoid blocking permission checks
        return 0;
    }
}