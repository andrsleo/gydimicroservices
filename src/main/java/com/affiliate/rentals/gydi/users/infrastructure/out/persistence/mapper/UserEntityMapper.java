package com.affiliate.rentals.gydi.users.infrastructure.out.persistence.mapper;

import java.util.Set;
import java.util.stream.Collectors;

import org.mapstruct.Mapper;
import org.mapstruct.Named;

import com.affiliate.rentals.gydi.users.domain.model.Email;
import com.affiliate.rentals.gydi.users.domain.model.Role;
import com.affiliate.rentals.gydi.users.domain.model.RoleName;
import com.affiliate.rentals.gydi.users.domain.model.User;
import com.affiliate.rentals.gydi.users.infrastructure.out.persistence.entity.RoleEntity;
import com.affiliate.rentals.gydi.users.infrastructure.out.persistence.entity.UserEntity;

/**
 * MapStruct mapper for converting between User domain model and UserEntity.
 *
 * <p>This mapper handles the bidirectional conversion between the domain layer's
 * User aggregate and the infrastructure layer's UserEntity. It properly maps
 * value objects like Email and Role to their JPA entity representations.</p>
 *
 * <p>The mapper is configured as a Spring component and uses custom methods
 * to handle complex mappings such as Email to String conversion and Role
 * to RoleEntity transformations.</p>
 *
 * @author GYDI Development Team
 * @see User
 * @see UserEntity
 */
@Mapper(componentModel = "spring")
public interface UserEntityMapper {

    /**
     * Converts a User domain model to a UserEntity.
     *
     * @param user the domain user to convert
     * @return the corresponding UserEntity
     */
    default UserEntity toEntity(User user) {
        if (user == null) {
            return null;
        }

        UserEntity entity = new UserEntity();
        entity.setId(user.id());
        entity.setName(user.name());
        entity.setEmail(emailToString(user.email()));
        entity.setPasswordHash(user.passwordHash());
        entity.setPhoneNumber(user.phoneNumber());
        entity.setRoles(rolesToRoleEntities(user.roles()));
        entity.setCreatedAt(user.createdAt());

        return entity;
    }

    /**
     * Converts a UserEntity to a User domain model.
     *
     * @param entity the UserEntity to convert
     * @return the corresponding User domain model
     */
    default User toDomain(UserEntity entity) {
        if (entity == null) {
            return null;
        }

        return User.builder()
                .id(entity.getId())
                .name(entity.getName())
                .email(stringToEmail(entity.getEmail()))
                .passwordHash(entity.getPasswordHash())
                .phoneNumber(entity.getPhoneNumber())
                .roles(roleEntitiesToRoles(entity.getRoles()))
                .createdAt(entity.getCreatedAt())
                .build();
    }

    /**
     * Converts an Email value object to a String.
     *
     * @param email the Email value object
     * @return the email address as a String
     */
    @Named("emailToString")
    default String emailToString(Email email) {
        return email != null ? email.address() : null;
    }

    /**
     * Converts a String to an Email value object.
     *
     * @param email the email address as a String
     * @return the Email value object
     */
    @Named("stringToEmail")
    default Email stringToEmail(String email) {
        return email != null ? Email.of(email) : null;
    }

    /**
     * Converts a Set of Role domain models to a Set of RoleEntities.
     *
     * @param roles the Set of Role domain models
     * @return the corresponding Set of RoleEntities
     */
    @Named("rolesToRoleEntities")
    default Set<RoleEntity> rolesToRoleEntities(Set<Role> roles) {
        if (roles == null) {
            return Set.of();
        }
        return roles.stream()
                .map(role -> new RoleEntity(role.id(), role.name().name()))
                .collect(Collectors.toSet());
    }

    /**
     * Converts a Set of RoleEntities to a Set of Role domain models.
     *
     * @param roleEntities the Set of RoleEntities
     * @return the corresponding Set of Role domain models
     */
    @Named("roleEntitiesToRoles")
    default Set<Role> roleEntitiesToRoles(Set<RoleEntity> roleEntities) {
        if (roleEntities == null) {
            return Set.of();
        }
        return roleEntities.stream()
                .map(entity -> new Role(entity.getId(), RoleName.fromValue(entity.getName())))
                .collect(Collectors.toSet());
    }
}
