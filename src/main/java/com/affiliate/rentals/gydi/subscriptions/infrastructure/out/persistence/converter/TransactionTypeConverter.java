package com.affiliate.rentals.gydi.subscriptions.infrastructure.out.persistence.converter;

import com.affiliate.rentals.gydi.subscriptions.infrastructure.out.persistence.entity.SubscriptionTransactionEntity.TransactionTypeEntity;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA AttributeConverter for mapping TransactionTypeEntity enum to PostgreSQL ENUM type.
 */
@Converter(autoApply = true)
public class TransactionTypeConverter implements AttributeConverter<TransactionTypeEntity, String> {

    @Override
    public String convertToDatabaseColumn(TransactionTypeEntity attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public TransactionTypeEntity convertToEntityAttribute(String dbData) {
        return dbData == null ? null : TransactionTypeEntity.valueOf(dbData);
    }
}
