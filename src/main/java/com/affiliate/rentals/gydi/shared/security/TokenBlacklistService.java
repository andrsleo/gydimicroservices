package com.affiliate.rentals.gydi.shared.security;

import com.affiliate.rentals.gydi.users.infrastructure.out.persistence.entity.TokenBlacklistEntity;
import com.affiliate.rentals.gydi.users.infrastructure.out.persistence.repository.TokenBlacklistJpaRepository;
import io.jsonwebtoken.Claims;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Date;

/**
 * Service for managing JWT token blacklist.
 *
 * <p>This service provides functionality to blacklist JWT tokens (for logout)
 * and check if tokens are blacklisted. It also includes automatic cleanup
 * of expired blacklisted tokens.</p>
 *
 * @author GYDI Development Team
 */
@Service
public class TokenBlacklistService {

    private final TokenBlacklistJpaRepository blacklistRepository;
    private final JwtService jwtService;

    public TokenBlacklistService(
            TokenBlacklistJpaRepository blacklistRepository,
            JwtService jwtService
    ) {
        this.blacklistRepository = blacklistRepository;
        this.jwtService = jwtService;
    }

    /**
     * Blacklists a JWT token.
     *
     * @param token the JWT token to blacklist
     */
    @Transactional
    public void blacklistToken(String token) {
        // Extract expiry date from token
        Date expiryDate = jwtService.extractClaim(token, Claims::getExpiration);

        TokenBlacklistEntity entity = new TokenBlacklistEntity();
        entity.setToken(token);
        entity.setExpiryDate(expiryDate.toInstant());

        blacklistRepository.save(entity);
    }

    /**
     * Checks if a token is blacklisted.
     *
     * @param token the JWT token to check
     * @return true if blacklisted, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean isBlacklisted(String token) {
        return blacklistRepository.existsByToken(token);
    }

    /**
     * Scheduled task to clean up expired blacklisted tokens.
     * Runs daily at 3 AM.
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupExpiredTokens() {
        blacklistRepository.deleteExpiredTokens(Instant.now());
    }
}