package com.affiliate.rentals.gydi.subscriptions.infrastructure.out.persistence.converter;

import com.affiliate.rentals.gydi.subscriptions.infrastructure.out.persistence.entity.UserSubscriptionEntity.SubscriptionStatusEntity;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA AttributeConverter for mapping SubscriptionStatusEntity enum to PostgreSQL ENUM type.
 *
 * <p>This converter handles the bidirectional conversion between the Java enum
 * {@link SubscriptionStatusEntity} and the PostgreSQL custom ENUM type
 * {@code subscriptions.subscription_status}.</p>
 *
 * <p>Using {@code @Converter(autoApply = true)} ensures this converter is automatically
 * applied to all entity attributes of type {@link SubscriptionStatusEntity}.</p>
 *
 * @author GYDI Development Team
 */
@Converter(autoApply = true)
public class SubscriptionStatusConverter implements AttributeConverter<SubscriptionStatusEntity, String> {

    @Override
    public String convertToDatabaseColumn(SubscriptionStatusEntity attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public SubscriptionStatusEntity convertToEntityAttribute(String dbData) {
        return dbData == null ? null : SubscriptionStatusEntity.valueOf(dbData);
    }
}
