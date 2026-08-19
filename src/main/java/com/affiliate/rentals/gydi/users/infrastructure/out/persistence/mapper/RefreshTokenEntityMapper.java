package com.affiliate.rentals.gydi.users.infrastructure.out.persistence.mapper;

import com.affiliate.rentals.gydi.users.domain.model.RefreshToken;
import com.affiliate.rentals.gydi.users.infrastructure.out.persistence.entity.RefreshTokenEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between RefreshToken domain model and RefreshTokenEntity.
 *
 * @author GYDI Development Team
 */
@Component
public class RefreshTokenEntityMapper {

    /**
     * Converts a RefreshToken domain model to a RefreshTokenEntity.
     *
     * @param refreshToken the domain model
     * @return the entity
     */
    public RefreshTokenEntity toEntity(RefreshToken refreshToken) {
        if (refreshToken == null) {
            return null;
        }

        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setId(refreshToken.getId());
        entity.setToken(refreshToken.getToken());
        entity.setUserId(refreshToken.getUserId());
        entity.setTokenFamilyId(refreshToken.getTokenFamilyId());
        entity.setExpiryDate(refreshToken.getExpiryDate());
        entity.setCreatedAt(refreshToken.getCreatedAt());
        entity.setUsedAt(refreshToken.getUsedAt());
        entity.setRevokedAt(refreshToken.getRevokedAt());
        entity.setReplacedByToken(refreshToken.getReplacedByToken());
        return entity;
    }

    /**
     * Converts a RefreshTokenEntity to a RefreshToken domain model.
     *
     * @param entity the entity
     * @return the domain model
     */
    public RefreshToken toDomain(RefreshTokenEntity entity) {
        if (entity == null) {
            return null;
        }

        return RefreshToken.reconstitute(
                entity.getId(),
                entity.getToken(),
                entity.getUserId(),
                entity.getTokenFamilyId(),
                entity.getExpiryDate(),
                entity.getCreatedAt(),
                entity.getUsedAt(),
                entity.getRevokedAt(),
                entity.getReplacedByToken()
        );
    }
}