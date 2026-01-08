package com.affiliate.rentals.gydi.subscriptions.infrastructure.out.persistence.mapper;

import com.affiliate.rentals.gydi.subscriptions.domain.model.PaymentMethod;
import com.affiliate.rentals.gydi.subscriptions.domain.model.PaymentMethodType;
import com.affiliate.rentals.gydi.subscriptions.infrastructure.out.persistence.entity.PaymentMethodEntity;
import com.affiliate.rentals.gydi.subscriptions.infrastructure.out.persistence.entity.PaymentMethodEntity.PaymentMethodTypeEntity;
import org.springframework.stereotype.Component;

/**
 * Manual mapper for converting between PaymentMethod domain model and
 * PaymentMethodEntity.
 *
 * <p>
 * This mapper handles the bidirectional conversion between the domain layer's
 * PaymentMethod aggregate and the infrastructure layer's PaymentMethodEntity.
 * It properly maps domain enums to JPA entity enums.
 * </p>
 *
 * @author GYDI Development Team
 * @see PaymentMethod
 * @see PaymentMethodEntity
 */
@Component
public class PaymentMethodEntityMapper {

    /**
     * Converts a PaymentMethod domain model to a PaymentMethodEntity.
     *
     * @param paymentMethod the domain payment method to convert
     * @return the corresponding PaymentMethodEntity
     */
    public PaymentMethodEntity toEntity(PaymentMethod paymentMethod) {
        if (paymentMethod == null) {
            return null;
        }

        PaymentMethodEntity entity = new PaymentMethodEntity();
        entity.setId(paymentMethod.id());
        entity.setUserId(paymentMethod.userId());
        entity.setMethodType(toEntityType(paymentMethod.methodType()));
        entity.setGatewayProvider(paymentMethod.gatewayProvider());
        entity.setGatewayToken(paymentMethod.gatewayToken());
        entity.setCardLastFour(paymentMethod.cardLastFour());
        entity.setCardBrand(paymentMethod.cardBrand());
        entity.setCardExpMonth(paymentMethod.cardExpMonth());
        entity.setCardExpYear(paymentMethod.cardExpYear());
        entity.setBillingEmail(paymentMethod.billingEmail());
        entity.setIsDefault(paymentMethod.isDefault());
        entity.setIsActive(paymentMethod.isActive());
        entity.setCreatedAt(paymentMethod.createdAt());
        entity.setUpdatedAt(paymentMethod.updatedAt());

        return entity;
    }

    /**
     * Converts a PaymentMethodEntity to a PaymentMethod domain model.
     *
     * @param entity the PaymentMethodEntity to convert
     * @return the corresponding PaymentMethod domain model
     */
    public PaymentMethod toDomain(PaymentMethodEntity entity) {
        if (entity == null) {
            return null;
        }

        return PaymentMethod.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .methodType(toDomainType(entity.getMethodType()))
                .gatewayProvider(entity.getGatewayProvider())
                .gatewayToken(entity.getGatewayToken())
                .cardLastFour(entity.getCardLastFour())
                .cardBrand(entity.getCardBrand())
                .cardExpMonth(entity.getCardExpMonth())
                .cardExpYear(entity.getCardExpYear())
                .billingEmail(entity.getBillingEmail())
                .isDefault(entity.getIsDefault())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * Converts domain PaymentMethodType to entity PaymentMethodTypeEntity.
     */
    private PaymentMethodTypeEntity toEntityType(PaymentMethodType type) {
        if (type == null) {
            return null;
        }
        return PaymentMethodTypeEntity.valueOf(type.name());
    }

    /**
     * Converts entity PaymentMethodTypeEntity to domain PaymentMethodType.
     */
    private PaymentMethodType toDomainType(PaymentMethodTypeEntity type) {
        if (type == null) {
            return null;
        }
        return PaymentMethodType.valueOf(type.name());
    }
}
