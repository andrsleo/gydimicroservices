package com.affiliate.rentals.gydi.subscriptions.infrastructure.out.persistence.mapper;

import com.affiliate.rentals.gydi.subscriptions.domain.model.SubscriptionStatus;
import com.affiliate.rentals.gydi.subscriptions.domain.model.UserSubscription;
import com.affiliate.rentals.gydi.subscriptions.infrastructure.out.persistence.entity.UserSubscriptionEntity;
import com.affiliate.rentals.gydi.subscriptions.infrastructure.out.persistence.entity.UserSubscriptionEntity.SubscriptionStatusEntity;
import org.springframework.stereotype.Component;

/**
 * Manual mapper for converting between UserSubscription domain model and UserSubscriptionEntity.
 *
 * <p>This mapper handles the bidirectional conversion between the domain layer's
 * UserSubscription aggregate and the infrastructure layer's UserSubscriptionEntity.
 * It properly maps domain enums to JPA entity enums.</p>
 *
 * @author GYDI Development Team
 * @see UserSubscription
 * @see UserSubscriptionEntity
 */
@Component
public class UserSubscriptionEntityMapper {

    /**
     * Converts a UserSubscription domain model to a UserSubscriptionEntity.
     *
     * @param subscription the domain subscription to convert
     * @return the corresponding UserSubscriptionEntity
     */
    public UserSubscriptionEntity toEntity(UserSubscription subscription) {
        if (subscription == null) {
            return null;
        }

        UserSubscriptionEntity entity = new UserSubscriptionEntity();
        entity.setId(subscription.id());
        entity.setUserId(subscription.userId());
        entity.setPlanId(subscription.planId());
        entity.setStatus(toEntityStatus(subscription.status()));
        entity.setStartedAt(subscription.startedAt());
        entity.setExpiresAt(subscription.expiresAt());
        entity.setCanceledAt(subscription.canceledAt());
        entity.setCancelationReason(subscription.cancelationReason());
        entity.setPaymentMethodId(subscription.paymentMethodId());
        entity.setAutoRenew(subscription.autoRenew());
        entity.setLastRenewalAt(subscription.lastRenewalAt());
        entity.setNextBillingDate(subscription.nextBillingDate());
        entity.setStripeSubscriptionId(subscription.stripeSubscriptionId());
        entity.setCreatedAt(subscription.createdAt());
        entity.setUpdatedAt(subscription.updatedAt());

        return entity;
    }

    /**
     * Converts a UserSubscriptionEntity to a UserSubscription domain model.
     *
     * @param entity the UserSubscriptionEntity to convert
     * @return the corresponding UserSubscription domain model
     */
    public UserSubscription toDomain(UserSubscriptionEntity entity) {
        if (entity == null) {
            return null;
        }

        return UserSubscription.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .planId(entity.getPlanId())
                .status(toDomainStatus(entity.getStatus()))
                .startedAt(entity.getStartedAt())
                .expiresAt(entity.getExpiresAt())
                .canceledAt(entity.getCanceledAt())
                .cancelationReason(entity.getCancelationReason())
                .paymentMethodId(entity.getPaymentMethodId())
                .autoRenew(entity.getAutoRenew())
                .lastRenewalAt(entity.getLastRenewalAt())
                .nextBillingDate(entity.getNextBillingDate())
                .stripeSubscriptionId(entity.getStripeSubscriptionId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * Converts domain SubscriptionStatus to entity SubscriptionStatusEntity.
     */
    private SubscriptionStatusEntity toEntityStatus(SubscriptionStatus status) {
        if (status == null) {
            return null;
        }
        return SubscriptionStatusEntity.valueOf(status.name());
    }

    /**
     * Converts entity SubscriptionStatusEntity to domain SubscriptionStatus.
     */
    private SubscriptionStatus toDomainStatus(SubscriptionStatusEntity status) {
        if (status == null) {
            return null;
        }
        return SubscriptionStatus.valueOf(status.name());
    }
}
