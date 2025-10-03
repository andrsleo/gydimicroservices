package com.affiliate.rentals.gydi.users.application.mapper;

import com.affiliate.rentals.gydi.users.application.dto.UserResponse;
import com.affiliate.rentals.gydi.users.domain.model.Role;
import com.affiliate.rentals.gydi.users.domain.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * MapStruct mapper for converting between User domain entities and DTOs.
 *
 * <p>This mapper provides automatic mapping between domain models and application layer DTOs,
 * reducing boilerplate code while maintaining type safety.</p>
 *
 * @author GYDI Development Team
 */
@Mapper(componentModel = "spring")
public interface UserDtoMapper {

    /**
     * Maps a User domain entity to a UserResponse DTO.
     *
     * @param user the domain user entity
     * @return the corresponding UserResponse DTO
     */
    @Mapping(target = "id", expression = "java(user.id())")
    @Mapping(target = "email", expression = "java(user.email().address())")
    @Mapping(target = "name", expression = "java(user.name())")
    @Mapping(target = "phoneNumber", expression = "java(user.phoneNumber())")
    @Mapping(target = "roleNames", expression = "java(mapRolesToNames(user.roles()))")
    @Mapping(target = "createdAt", expression = "java(user.createdAt())")
    UserResponse toResponse(User user);

    /**
     * Maps a set of Role value objects to role name strings.
     *
     * @param roles the set of roles
     * @return the set of role names
     */
    default Set<String> mapRolesToNames(Set<Role> roles) {
        return roles.stream()
                .map(role -> role.name().getValue())
                .collect(Collectors.toSet());
    }
}
