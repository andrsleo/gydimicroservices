package com.affiliate.rentals.gydi.users.infrastructure.out.persistence.adapter;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.affiliate.rentals.gydi.users.domain.model.RefreshToken;
import com.affiliate.rentals.gydi.users.domain.ports.RefreshTokenRepositoryPort;
import com.affiliate.rentals.gydi.users.infrastructure.out.persistence.mapper.RefreshTokenEntityMapper;
import com.affiliate.rentals.gydi.users.infrastructure.out.persistence.repository.RefreshTokenJpaRepository;

/**
 * Adapter implementation of RefreshTokenRepository port with rotation support.
 *
 * <p>This adapter implements the RefreshTokenRepository port using Spring Data JPA,
 * bridging the domain layer with the persistence infrastructure. Supports OWASP-recommended
 * token rotation and reuse detection.</p>
 *
 * @author GYDI Development Team
 */
@Component
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepositoryPort {

    private final RefreshTokenJpaRepository jpaRepository;
    private final RefreshTokenEntityMapper mapper;

    public RefreshTokenRepositoryAdapter(
            RefreshTokenJpaRepository jpaRepository,
            RefreshTokenEntityMapper mapper
    ) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public RefreshToken save(RefreshToken refreshToken) {
        var entity = mapper.toEntity(refreshToken);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RefreshToken> findByToken(String token) {
        return jpaRepository.findByToken(token)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RefreshToken> findByUserId(Long userId) {
        return jpaRepository.findByUserId(userId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RefreshToken> findByTokenFamilyId(UUID tokenFamilyId) {
        return jpaRepository.findByTokenFamilyId(tokenFamilyId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public int revokeAllByUserId(Long userId) {
        return jpaRepository.revokeAllByUserId(userId, Instant.now());
    }

    @Override
    @Transactional
    public int revokeAllByTokenFamily(UUID tokenFamilyId) {
        return jpaRepository.revokeAllByTokenFamily(tokenFamilyId, Instant.now());
    }

    @Override
    @Transactional
    public void deleteExpiredTokens() {
        jpaRepository.deleteExpiredTokens(Instant.now());
    }
}