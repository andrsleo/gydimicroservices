package com.affiliate.rentals.gydi.users.application.usecase;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.affiliate.rentals.gydi.shared.security.JwtService;
import com.affiliate.rentals.gydi.users.application.dto.AuthResponse;
import com.affiliate.rentals.gydi.users.application.dto.RefreshTokenRequest;
import com.affiliate.rentals.gydi.users.application.dto.UserResponse;
import com.affiliate.rentals.gydi.users.application.mapper.UserDtoMapper;
import com.affiliate.rentals.gydi.users.domain.exception.InvalidRefreshTokenException;
import com.affiliate.rentals.gydi.users.domain.exception.RefreshTokenReusedException;
import com.affiliate.rentals.gydi.users.domain.exception.UserNotFoundException;
import com.affiliate.rentals.gydi.users.domain.model.RefreshToken;
import com.affiliate.rentals.gydi.users.domain.model.User;
import com.affiliate.rentals.gydi.users.domain.ports.RefreshTokenRepositoryPort;
import com.affiliate.rentals.gydi.users.domain.ports.UserRepositoryPort;

import lombok.extern.slf4j.Slf4j;


/**
 * Use case for refreshing JWT access tokens using a refresh token with automatic rotation.
 *
 * <p>This use case implements OWASP-recommended token rotation security:</p>
 * <ul>
 *   <li><b>One-Time Use:</b> Each refresh token can only be used once</li>
 *   <li><b>Automatic Rotation:</b> New token issued on each refresh</li>
 *   <li><b>Reuse Detection:</b> Attempting to reuse a token triggers security response</li>
 *   <li><b>Family Revocation:</b> Token reuse revokes entire rotation chain</li>
 * </ul>
 *
 * @author GYDI Development Team
 */
@Slf4j
@Service
public class RefreshTokenUseCase {

    private final RefreshTokenRepositoryPort refreshTokenRepository;
    private final UserRepositoryPort userRepository;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final UserDtoMapper mapper;

    public RefreshTokenUseCase(
            RefreshTokenRepositoryPort refreshTokenRepository,
            UserRepositoryPort userRepository,
            JwtService jwtService,
            UserDetailsService userDetailsService,
            UserDtoMapper mapper
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.mapper = mapper;
    }

    /**
     * Executes the refresh token use case with automatic rotation.
     *
     * <p><b>Security Flow:</b></p>
     * <ol>
     *   <li>Validate token exists in database</li>
     *   <li>Check if token was already used (SECURITY: reuse detection)</li>
     *   <li>If reused, revoke entire token family and throw exception</li>
     *   <li>Check if token is expired or revoked</li>
     *   <li>Mark current token as used</li>
     *   <li>Generate new refresh token in same family</li>
     *   <li>Generate new access token</li>
     *   <li>Return both new tokens</li>
     * </ol>
     *
     * @param request the refresh token request
     * @return an AuthResponse with new access token and new refresh token
     * @throws InvalidRefreshTokenException if the refresh token is invalid or expired
     * @throws RefreshTokenReusedException if the token has already been used (security breach)
     * @throws UserNotFoundException if the user associated with the token doesn't exist
     */
    @Transactional
    public AuthResponse execute(RefreshTokenRequest request) {
        // Step 1: Find the refresh token
        RefreshToken refreshToken = refreshTokenRepository
                .findByToken(request.refreshToken())
                .orElseThrow(() -> new InvalidRefreshTokenException("Invalid refresh token"));

        log.debug("Processing refresh token for user ID: {}, token family: {}",
                refreshToken.getUserId(), refreshToken.getTokenFamilyId());

        // Step 2: SECURITY CHECK - Detect token reuse (critical security feature)
        if (refreshToken.isUsed()) {
            log.warn("SECURITY: Token reuse detected! Token ID: {}, User ID: {}, Family: {}. Revoking entire token family.",
                    refreshToken.getId(), refreshToken.getUserId(), refreshToken.getTokenFamilyId());

            // Revoke ALL tokens in this family (rotation chain)
            int revokedCount = refreshTokenRepository.revokeAllByTokenFamily(refreshToken.getTokenFamilyId());
            log.warn("SECURITY: Revoked {} tokens in family {} due to token reuse",
                    revokedCount, refreshToken.getTokenFamilyId());

            // Throw exception - client must re-authenticate
            throw RefreshTokenReusedException.forToken(refreshToken.getId(), refreshToken.getUserId());
        }

        // Step 3: Check if token is expired or revoked
        if (refreshToken.isExpired()) {
            throw new InvalidRefreshTokenException("Refresh token has expired");
        }

        if (refreshToken.isRevoked()) {
            throw new InvalidRefreshTokenException("Refresh token has been revoked");
        }

        // Step 4: Find the user associated with the refresh token
        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> UserNotFoundException.withId(refreshToken.getUserId()));

        // Step 5: Create new refresh token in the same family (rotation)
        RefreshToken newRefreshToken = RefreshToken.createRotated(
                user.id(),
                refreshToken.getTokenFamilyId(), // Same family for tracking
                jwtService.getRefreshExpiration()
        );
        newRefreshToken = refreshTokenRepository.save(newRefreshToken);

        log.debug("Created new refresh token in same family. Old token: {}, New token: {}",
                refreshToken.getToken().substring(0, 8) + "...",
                newRefreshToken.getToken().substring(0, 8) + "...");

        // Step 6: Mark old token as used (one-time use enforcement)
        refreshToken.markAsUsed(newRefreshToken.getToken());
        refreshTokenRepository.save(refreshToken);

        log.debug("Marked old refresh token as used for user ID: {}", user.id());

        // Step 7: Generate new access token with complete user data
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.email().address());
        String newAccessToken = jwtService.generateTokenWithUserData(userDetails, user);

        // Step 8: Map user to response DTO
        UserResponse userResponse = mapper.toResponse(user);

        // Return response with BOTH new access token and new refresh token
        log.info("Successfully rotated refresh token for user ID: {}", user.id());
        return AuthResponse.of(newAccessToken, newRefreshToken.getToken(), userResponse);
    }
}