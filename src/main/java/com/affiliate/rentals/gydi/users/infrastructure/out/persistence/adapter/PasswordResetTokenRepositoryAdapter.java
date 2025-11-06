package com.affiliate.rentals.gydi.users.infrastructure.out.persistence.adapter;

import com.affiliate.rentals.gydi.users.domain.model.PasswordResetToken;
import com.affiliate.rentals.gydi.users.domain.ports.PasswordResetTokenRepositoryPort;
import com.affiliate.rentals.gydi.users.infrastructure.out.persistence.mapper.PasswordResetTokenEntityMapper;
import com.affiliate.rentals.gydi.users.infrastructure.out.persistence.repository.PasswordResetTokenJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adapter implementation of PasswordResetTokenRepositoryPort.
 *
 * <p>This adapter implements the PasswordResetTokenRepositoryPort using Spring Data JPA,
 * bridging the domain layer with the persistence infrastructure. It follows the
 * hexagonal architecture pattern, keeping the domain layer independent of infrastructure concerns.</p>
 *
 * <p>All validation is performed at the domain/port level. This adapter focuses solely
 * on data persistence operations.</p>
 *
 * @author GYDI Development Team
 */
@Component
public class PasswordResetTokenRepositoryAdapter implements PasswordResetTokenRepositoryPort {

    private final PasswordResetTokenJpaRepository jpaRepository;
    private final PasswordResetTokenEntityMapper mapper;

    public PasswordResetTokenRepositoryAdapter(
            PasswordResetTokenJpaRepository jpaRepository,
            PasswordResetTokenEntityMapper mapper
    ) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public PasswordResetToken save(PasswordResetToken token) {
        if (token == null) {
            throw new IllegalArgumentException("Token cannot be null");
        }

        var entity = mapper.toEntity(token);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PasswordResetToken> findByToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token string cannot be null or empty");
        }

        return jpaRepository.findByToken(token)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PasswordResetToken> findLatestValidTokenByUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        return jpaRepository.findLatestValidTokenByUserId(userId, LocalDateTime.now())
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PasswordResetToken> findAllByUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        return jpaRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public int invalidateAllTokensForUser(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        return jpaRepository.invalidateAllTokensForUser(userId, LocalDateTime.now());
    }

    @Override
    @Transactional
    public int deleteExpiredTokens() {
        return jpaRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }

    @Override
    @Transactional
    public int deleteUsedTokensOlderThan(int daysOld) {
        if (daysOld < 0) {
            throw new IllegalArgumentException("Days old cannot be negative");
        }

        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(daysOld);
        return jpaRepository.deleteByUsedTrueAndUsedAtBefore(cutoffTime);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasValidToken(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        return jpaRepository.hasValidToken(userId, LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public int countRequestsInLastHours(Long userId, int hoursBack) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (hoursBack < 0) {
            throw new IllegalArgumentException("Hours back cannot be negative");
        }

        LocalDateTime since = LocalDateTime.now().minusHours(hoursBack);
        return jpaRepository.countRequestsSince(userId, since);
    }
}
