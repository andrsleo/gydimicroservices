package com.affiliate.rentals.gydi.users.infrastructure.out.persistence.adapter;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.affiliate.rentals.gydi.users.domain.model.UserProfile;
import com.affiliate.rentals.gydi.users.domain.ports.UserProfileRepositoryPort;
import com.affiliate.rentals.gydi.users.infrastructure.out.persistence.mapper.UserProfileEntityMapper;
import com.affiliate.rentals.gydi.users.infrastructure.out.persistence.repository.JpaUserProfileRepository;

import lombok.RequiredArgsConstructor;

/**
 * Adapter implementation of UserProfileRepository port.
 *
 * <p>
 * This adapter bridges the domain layer and the persistence infrastructure,
 * implementing the UserProfileRepository port using Spring Data JPA and
 * PostgreSQL.
 * It follows the hexagonal architecture pattern by adapting the JPA repository
 * to the domain's port interface.
 * </p>
 *
 * @author GYDI Development Team
 * @see UserProfileRepository
 */
@Component
@RequiredArgsConstructor
public class UserProfileRepositoryAdapter implements UserProfileRepositoryPort {

    private final JpaUserProfileRepository jpaRepository;
    private final com.affiliate.rentals.gydi.users.infrastructure.out.persistence.repository.UserJpaRepository userJpaRepository;
    private final UserProfileEntityMapper mapper;

    @Override
    @Transactional
    public UserProfile save(UserProfile profile) {
        var entity = mapper.toEntity(profile);
        var savedEntity = jpaRepository.save(entity);

        // Sync phone number to User entity
        var userEntity = userJpaRepository.findById(profile.userId())
                .orElseThrow(() -> new IllegalStateException("User not found for profile: " + profile.userId()));

        userEntity.setPhoneNumber(profile.phoneNumber());
        userJpaRepository.save(userEntity);

        return mapper.toDomain(savedEntity, profile.phoneNumber());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserProfile> findById(Long id) {
        return jpaRepository.findById(id)
                .map(entity -> {
                    var userEntity = userJpaRepository.findById(entity.getUserId())
                            .orElse(null);
                    String phoneNumber = userEntity != null ? userEntity.getPhoneNumber() : null;
                    return mapper.toDomain(entity, phoneNumber);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserProfile> findByUserId(Long userId) {
        return jpaRepository.findByUserId(userId)
                .map(entity -> {
                    var userEntity = userJpaRepository.findById(userId)
                            .orElse(null);
                    String phoneNumber = userEntity != null ? userEntity.getPhoneNumber() : null;
                    return mapper.toDomain(entity, phoneNumber);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUserId(Long userId) {
        return jpaRepository.existsByUserId(userId);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void deleteByUserId(Long userId) {
        jpaRepository.deleteByUserId(userId);
    }
}