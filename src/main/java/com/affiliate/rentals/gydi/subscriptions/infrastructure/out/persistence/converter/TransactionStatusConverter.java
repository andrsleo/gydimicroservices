package com.affiliate.rentals.gydi.subscriptions.infrastructure.out.persistence.converter;

import com.affiliate.rentals.gydi.subscriptions.infrastructure.out.persistence.entity.SubscriptionTransactionEntity.TransactionStatusEntity;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA AttributeConverter for mapping TransactionStatusEntity enum to PostgreSQL ENUM type.
 */
@Converter(autoApply = true)
public class TransactionStatusConverter implements AttributeConverter<TransactionStatusEntity, String> {

    @Override
    public String convertToDatabaseColumn(TransactionStatusEntity attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public TransactionStatusEntity convertToEntityAttribute(String dbData) {
        return dbData == null ? null : TransactionStatusEntity.valueOf(dbData);
    }
}
