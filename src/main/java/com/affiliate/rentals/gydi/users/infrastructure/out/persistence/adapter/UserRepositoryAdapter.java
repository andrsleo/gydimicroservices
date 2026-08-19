package com.affiliate.rentals.gydi.users.infrastructure.out.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.affiliate.rentals.gydi.users.domain.model.Email;
import com.affiliate.rentals.gydi.users.domain.model.User;
import com.affiliate.rentals.gydi.users.domain.ports.UserRepositoryPort;
import com.affiliate.rentals.gydi.users.infrastructure.out.persistence.entity.RoleEntity;
import com.affiliate.rentals.gydi.users.infrastructure.out.persistence.entity.UserEntity;
import com.affiliate.rentals.gydi.users.infrastructure.out.persistence.mapper.UserEntityMapper;
import com.affiliate.rentals.gydi.users.infrastructure.out.persistence.repository.RoleJpaRepository;
import com.affiliate.rentals.gydi.users.infrastructure.out.persistence.repository.UserJpaRepository;

/**
 * Adapter implementation of the UserRepository port using JPA.
 *
 * <p>
 * This adapter implements the hexagonal architecture's port interface,
 * bridging the domain layer with the infrastructure persistence layer.
 * It uses JPA repositories and MapStruct mappers to persist and retrieve
 * User domain models.
 * </p>
 *
 * <p>
 * The adapter handles the complexity of managing the many-to-many
 * relationship between users and roles, ensuring proper role persistence
 * and association.
 * </p>
 *
 * @author GYDI Development Team
 * @see UserRepository
 * @see UserJpaRepository
 * @see RoleJpaRepository
 * @see UserEntityMapper
 */
@Component
public class UserRepositoryAdapter implements UserRepositoryPort {

    private final UserJpaRepository userJpaRepository;
    private final RoleJpaRepository roleJpaRepository;
    private final com.affiliate.rentals.gydi.users.infrastructure.out.persistence.repository.JpaUserProfileRepository userProfileRepository;
    private final UserEntityMapper mapper;

    /**
     * Constructs a new UserRepositoryAdapter with required dependencies.
     *
     * @param userJpaRepository     the JPA repository for users
     * @param roleJpaRepository     the JPA repository for roles
     * @param userProfileRepository the JPA repository for user profiles
     * @param mapper                the mapper for User-UserEntity conversion
     */
    public UserRepositoryAdapter(
            UserJpaRepository userJpaRepository,
            RoleJpaRepository roleJpaRepository,
            com.affiliate.rentals.gydi.users.infrastructure.out.persistence.repository.JpaUserProfileRepository userProfileRepository,
            UserEntityMapper mapper) {
        this.userJpaRepository = userJpaRepository;
        this.roleJpaRepository = roleJpaRepository;
        this.userProfileRepository = userProfileRepository;
        this.mapper = mapper;
    }

    private String resolveName(UserEntity entity) {
        return userProfileRepository.findByUserId(entity.getId())
                .map(profile -> {
                    String firstName = profile.getFirstName();
                    String lastName = profile.getLastName();
                    if (firstName != null && lastName != null) {
                        return firstName + " " + lastName;
                    } else if (firstName != null) {
                        return firstName;
                    } else if (lastName != null) {
                        return lastName;
                    }
                    return null;
                })
                .orElse(entity.getEmail()); // Fallback to email if name not found
    }

    @Override
    @Transactional
    public User save(User user) {
        UserEntity entity = mapper.toEntity(user);

        // Resolve and attach managed role entities
        Set<RoleEntity> managedRoles = entity.getRoles().stream()
                .map(role -> roleJpaRepository.findByName(role.getName())
                        .orElseGet(() -> roleJpaRepository.save(new RoleEntity(role.getName()))))
                .collect(Collectors.toSet());

        entity.setRoles(managedRoles);

        UserEntity savedEntity = userJpaRepository.save(entity);

        // For new users, profile might not exist yet, so use the name from the domain
        // object if available
        // or fallback to resolveName (which falls back to email)
        String name = user.name();
        if (name == null || name.isBlank()) {
            name = resolveName(savedEntity);
        }

        return mapper.toDomain(savedEntity, name);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return userJpaRepository.findById(id)
                .map(entity -> mapper.toDomain(entity, resolveName(entity)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(Email email) {
        return userJpaRepository.findByEmail(email.address())
                .map(entity -> mapper.toDomain(entity, resolveName(entity)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userJpaRepository.findAllWithRoles().stream()
                .map(entity -> mapper.toDomain(entity, resolveName(entity)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findByRole(String roleName) {
        return userJpaRepository.findByRoleName(roleName).stream()
                .map(entity -> mapper.toDomain(entity, resolveName(entity)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(Email email) {
        return userJpaRepository.existsByEmail(email.address());
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        userJpaRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return userJpaRepository.count();
    }
}
