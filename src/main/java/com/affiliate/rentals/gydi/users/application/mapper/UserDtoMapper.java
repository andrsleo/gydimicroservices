package com.affiliate.rentals.gydi.users.application.mapper;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.affiliate.rentals.gydi.users.application.dto.UserResponse;
import com.affiliate.rentals.gydi.users.domain.model.User;

/**
 * Manual mapper for converting between User domain entities and DTOs.
 *
 * <p>This mapper provides mapping between domain models and application layer DTOs,
 * maintaining type safety and clean separation of concerns.</p>
 *
 * @author GYDI Development Team
 */
@Component
public class UserDtoMapper {

    /**
     * Maps a User domain entity to a UserResponse DTO.
     *
     * @param user the domain user entity
     * @return the corresponding UserResponse DTO
     */
    public UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }

        Set<String> roleNames = Set.of();
        if (user.roles() != null) {
            roleNames = user.roles().stream()
                .map(role -> role.name().getValue())
                .collect(Collectors.toSet());
        }

        return new UserResponse(
            user.id(),
            user.email() != null ? user.email().address() : null,
            user.name(),
            user.phoneNumber(),
            roleNames,
            user.createdAt()
        );
    }
}
