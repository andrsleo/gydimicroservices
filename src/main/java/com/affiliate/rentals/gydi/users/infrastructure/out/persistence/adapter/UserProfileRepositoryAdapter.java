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
    private final UserProfileEntityMapper mapper;

    @Override
    @Transactional
    public UserProfile save(UserProfile profile) {
        var entity = mapper.toEntity(profile);
        var savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserProfile> findById(Long id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserProfile> findByUserId(Long userId) {
        return jpaRepository.findByUserId(userId)
                .map(mapper::toDomain);
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