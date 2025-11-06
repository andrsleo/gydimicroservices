package com.affiliate.rentals.gydi.users.infrastructure.out.persistence.mapper;

import com.affiliate.rentals.gydi.users.domain.model.PasswordResetToken;
import com.affiliate.rentals.gydi.users.infrastructure.out.persistence.entity.PasswordResetTokenEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between PasswordResetToken domain model and PasswordResetTokenEntity.
 *
 * <p>This mapper bridges the gap between the domain layer and the infrastructure layer,
 * following hexagonal architecture principles. It handles bidirectional conversions
 * while maintaining separation of concerns.</p>
 *
 * @author GYDI Development Team
 */
@Component
public class PasswordResetTokenEntityMapper {

    /**
     * Converts a PasswordResetToken domain model to a PasswordResetTokenEntity.
     *
     * @param token the domain model
     * @return the entity, or null if input is null
     */
    public PasswordResetTokenEntity toEntity(PasswordResetToken token) {
        if (token == null) {
            return null;
        }

        PasswordResetTokenEntity entity = new PasswordResetTokenEntity();
        entity.setId(token.id());
        entity.setUserId(token.userId());
        entity.setToken(token.token());
        entity.setExpiresAt(token.expiresAt());
        entity.setUsed(token.isUsed());
        entity.setCreatedAt(token.createdAt());
        entity.setUsedAt(token.usedAt());
        entity.setIpAddress(token.ipAddress());
        return entity;
    }

    /**
     * Converts a PasswordResetTokenEntity to a PasswordResetToken domain model.
     *
     * @param entity the entity
     * @return the domain model, or null if input is null
     */
    public PasswordResetToken toDomain(PasswordResetTokenEntity entity) {
        if (entity == null) {
            return null;
        }

        return PasswordResetToken.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .token(entity.getToken())
                .expiresAt(entity.getExpiresAt())
                .used(entity.getUsed())
                .createdAt(entity.getCreatedAt())
                .usedAt(entity.getUsedAt())
                .ipAddress(entity.getIpAddress())
                .build();
    }
}
