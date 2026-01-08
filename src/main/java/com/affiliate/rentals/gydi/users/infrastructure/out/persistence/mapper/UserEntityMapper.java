package com.affiliate.rentals.gydi.users.infrastructure.out.persistence.mapper;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.affiliate.rentals.gydi.users.domain.model.*;
import com.affiliate.rentals.gydi.users.infrastructure.out.persistence.entity.RoleEntity;
import com.affiliate.rentals.gydi.users.infrastructure.out.persistence.entity.SubscriptionPlanEntity;
import com.affiliate.rentals.gydi.users.infrastructure.out.persistence.entity.UserEntity;

/**
 * Manual mapper for converting between User domain model and UserEntity.
 *
 * <p>
 * This mapper handles the bidirectional conversion between the domain layer's
 * User aggregate and the infrastructure layer's UserEntity. It properly maps
 * value objects like Email and Role to their JPA entity representations.
 * </p>
 *
 * @author GYDI Development Team
 * @see User
 * @see UserEntity
 */
@Component
public class UserEntityMapper {

    /**
     * Converts a User domain model to a UserEntity.
     *
     * @param user the domain user to convert
     * @return the corresponding UserEntity
     */
    public UserEntity toEntity(User user) {
        if (user == null) {
            return null;
        }

        UserEntity entity = new UserEntity();
        entity.setId(user.id());

        entity.setEmail(user.email() != null ? user.email().address() : null);
        entity.setPasswordHash(user.passwordHash());
        entity.setPhoneNumber(user.phoneNumber());
        entity.setCreatedAt(user.createdAt());

        // Map subscription plan
        if (user.activePlan() != null) {
            entity.setActivePlan(SubscriptionPlanEntity.valueOf(user.activePlan().name()));
        }

        // Map capabilities
        if (user.capabilities() != null) {
            entity.setCanPublish(user.capabilities().canPublish());
            entity.setCanRefer(user.capabilities().canRefer());
            entity.setCanRent(user.capabilities().canRent());
        }

        // Map account verification
        entity.setAccountVerified(user.isAccountVerified());

        // Map Stripe Customer ID
        entity.setStripeCustomerId(user.stripeCustomerId());

        // Map roles
        if (user.roles() != null) {
            Set<RoleEntity> roleEntities = user.roles().stream()
                    .map(role -> new RoleEntity(role.id(), role.name().name()))
                    .collect(Collectors.toSet());
            entity.setRoles(roleEntities);
        }

        return entity;
    }

    /**
     * Converts a UserEntity to a User domain model.
     *
     * @param entity the UserEntity to convert
     * @param name   the user's name (from UserProfile)
     * @return the corresponding User domain model
     */
    public User toDomain(UserEntity entity, String name) {
        if (entity == null) {
            return null;
        }

        // Map roles
        Set<Role> roles = Set.of();
        if (entity.getRoles() != null) {
            roles = entity.getRoles().stream()
                    .map(roleEntity -> new Role(
                            roleEntity.getId(),
                            RoleName.fromValue(roleEntity.getName())))
                    .collect(Collectors.toSet());
        }

        // Map subscription plan
        SubscriptionPlan activePlan = entity.getActivePlan() != null
                ? SubscriptionPlan.valueOf(entity.getActivePlan().name())
                : SubscriptionPlan.FREE;

        // Map capabilities
        UserCapabilities capabilities = UserCapabilities.of(
                entity.getCanPublish() != null ? entity.getCanPublish() : true,
                entity.getCanRefer() != null ? entity.getCanRefer() : true,
                entity.getCanRent() != null ? entity.getCanRent() : true);

        return User.builder()
                .id(entity.getId())
                .name(name)
                .email(entity.getEmail() != null ? Email.of(entity.getEmail()) : null)
                .passwordHash(entity.getPasswordHash())
                .phoneNumber(entity.getPhoneNumber())
                .roles(roles)
                .activePlan(activePlan)
                .capabilities(capabilities)
                .accountVerified(entity.getAccountVerified() != null ? entity.getAccountVerified() : false)
                .stripeCustomerId(entity.getStripeCustomerId())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
