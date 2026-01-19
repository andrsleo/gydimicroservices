package com.affiliate.rentals.gydi.subscriptions.infrastructure.out.persistence.mapper;

import com.affiliate.rentals.gydi.subscriptions.domain.model.SubscriptionTransaction;
import com.affiliate.rentals.gydi.subscriptions.domain.model.TransactionStatus;
import com.affiliate.rentals.gydi.subscriptions.domain.model.TransactionType;
import com.affiliate.rentals.gydi.subscriptions.infrastructure.out.persistence.entity.SubscriptionTransactionEntity;
import com.affiliate.rentals.gydi.subscriptions.infrastructure.out.persistence.entity.SubscriptionTransactionEntity.TransactionStatusEntity;
import com.affiliate.rentals.gydi.subscriptions.infrastructure.out.persistence.entity.SubscriptionTransactionEntity.TransactionTypeEntity;
import org.springframework.stereotype.Component;

/**
 * Manual mapper for converting between SubscriptionTransaction domain model and SubscriptionTransactionEntity.
 *
 * <p>This mapper handles the bidirectional conversion between the domain layer's
 * SubscriptionTransaction aggregate and the infrastructure layer's SubscriptionTransactionEntity.
 * It properly maps domain enums to JPA entity enums.</p>
 *
 * @author GYDI Development Team
 * @see SubscriptionTransaction
 * @see SubscriptionTransactionEntity
 */
@Component
public class SubscriptionTransactionEntityMapper {

    /**
     * Converts a SubscriptionTransaction domain model to a SubscriptionTransactionEntity.
     *
     * @param transaction the domain transaction to convert
     * @return the corresponding SubscriptionTransactionEntity
     */
    public SubscriptionTransactionEntity toEntity(SubscriptionTransaction transaction) {
        if (transaction == null) {
            return null;
        }

        SubscriptionTransactionEntity entity = new SubscriptionTransactionEntity();
        entity.setId(transaction.id());
        entity.setUserSubscriptionId(transaction.userSubscriptionId());
        entity.setUserId(transaction.userId());
        entity.setPaymentMethodId(transaction.paymentMethodId());
        entity.setTransactionType(toEntityType(transaction.transactionType()));
        entity.setTransactionStatus(toEntityStatus(transaction.transactionStatus()));
        entity.setFromPlanId(transaction.fromPlanId());
        entity.setToPlanId(transaction.toPlanId());
        entity.setAmount(transaction.amount());
        entity.setCurrency(transaction.currency());
        entity.setStripePaymentIntentId(transaction.stripePaymentIntentId());
        entity.setStripeChargeId(transaction.stripeChargeId());
        entity.setStripeInvoiceId(transaction.stripeInvoiceId());
        entity.setGatewayResponse(transaction.gatewayResponse());
        entity.setFailureReason(transaction.failureReason());
        entity.setPeriodStart(transaction.periodStart());
        entity.setPeriodEnd(transaction.periodEnd());
        entity.setProcessedAt(transaction.processedAt());
        entity.setCreatedAt(transaction.createdAt());
        entity.setUpdatedAt(transaction.updatedAt());

        return entity;
    }

    /**
     * Converts a SubscriptionTransactionEntity to a SubscriptionTransaction domain model.
     *
     * @param entity the SubscriptionTransactionEntity to convert
     * @return the corresponding SubscriptionTransaction domain model
     */
    public SubscriptionTransaction toDomain(SubscriptionTransactionEntity entity) {
        if (entity == null) {
            return null;
        }

        return SubscriptionTransaction.builder()
                .id(entity.getId())
                .userSubscriptionId(entity.getUserSubscriptionId())
                .userId(entity.getUserId())
                .paymentMethodId(entity.getPaymentMethodId())
                .transactionType(toDomainType(entity.getTransactionType()))
                .transactionStatus(toDomainStatus(entity.getTransactionStatus()))
                .fromPlanId(entity.getFromPlanId())
                .toPlanId(entity.getToPlanId())
                .amount(entity.getAmount())
                .currency(entity.getCurrency())
                .stripePaymentIntentId(entity.getStripePaymentIntentId())
                .stripeChargeId(entity.getStripeChargeId())
                .stripeInvoiceId(entity.getStripeInvoiceId())
                .gatewayResponse(entity.getGatewayResponse())
                .failureReason(entity.getFailureReason())
                .periodStart(entity.getPeriodStart())
                .periodEnd(entity.getPeriodEnd())
                .processedAt(entity.getProcessedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * Converts domain TransactionType to entity TransactionTypeEntity.
     */
    private TransactionTypeEntity toEntityType(TransactionType type) {
        if (type == null) {
            return null;
        }
        return TransactionTypeEntity.valueOf(type.name());
    }

    /**
     * Converts entity TransactionTypeEntity to domain TransactionType.
     */
    private TransactionType toDomainType(TransactionTypeEntity type) {
        if (type == null) {
            return null;
        }
        return TransactionType.valueOf(type.name());
    }

    /**
     * Converts domain TransactionStatus to entity TransactionStatusEntity.
     */
    private TransactionStatusEntity toEntityStatus(TransactionStatus status) {
        if (status == null) {
            return null;
        }
        return TransactionStatusEntity.valueOf(status.name());
    }

    /**
     * Converts entity TransactionStatusEntity to domain TransactionStatus.
     */
    private TransactionStatus toDomainStatus(TransactionStatusEntity status) {
        if (status == null) {
            return null;
        }
        return TransactionStatus.valueOf(status.name());
    }
}
