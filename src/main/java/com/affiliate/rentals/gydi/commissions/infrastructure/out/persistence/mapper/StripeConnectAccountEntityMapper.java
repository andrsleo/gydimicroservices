package com.affiliate.rentals.gydi.commissions.infrastructure.out.persistence.mapper;

import com.affiliate.rentals.gydi.commissions.domain.model.StripeConnectAccount;
import com.affiliate.rentals.gydi.commissions.infrastructure.out.persistence.entity.StripeConnectAccountEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper between StripeConnectAccount (domain) and StripeConnectAccountEntity (JPA).
 */
@Component
public class StripeConnectAccountEntityMapper {

    /**
     * Maps domain model to JPA entity.
     */
    public StripeConnectAccountEntity toEntity(StripeConnectAccount domain) {
        if (domain == null) {
            return null;
        }

        return StripeConnectAccountEntity.builder()
            .id(domain.getId())
            .userId(domain.getUserId())
            .stripeAccountId(domain.getStripeAccountId())
            .accountType(domain.getAccountType())
            .onboardingCompleted(domain.isOnboardingCompleted())
            .detailsSubmitted(domain.isDetailsSubmitted())
            .payoutsEnabled(domain.isPayoutsEnabled())
            .chargesEnabled(domain.isChargesEnabled())
            .country(domain.getCountry())
            .verificationStatus(domain.getVerificationStatus())
            .stripePlatformCustomerId(domain.getStripePlatformCustomerId())
            .createdAt(domain.getCreatedAt())
            .updatedAt(domain.getUpdatedAt())
            .build();
    }

    /**
     * Maps JPA entity to domain model.
     */
    public StripeConnectAccount toDomain(StripeConnectAccountEntity entity) {
        if (entity == null) {
            return null;
        }

        return StripeConnectAccount.reconstitute(
            entity.getId(),
            entity.getUserId(),
            entity.getStripeAccountId(),
            entity.getAccountType(),
            entity.getOnboardingCompleted(),
            entity.getDetailsSubmitted(),
            entity.getPayoutsEnabled(),
            entity.getChargesEnabled(),
            entity.getCountry(),
            entity.getVerificationStatus(),
            entity.getStripePlatformCustomerId(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
