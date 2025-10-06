package com.affiliate.rentals.gydi.users.application.usecase;

import com.affiliate.rentals.gydi.shared.security.JwtService;
import com.affiliate.rentals.gydi.users.application.dto.AuthResponse;
import com.affiliate.rentals.gydi.users.application.dto.RefreshTokenRequest;
import com.affiliate.rentals.gydi.users.application.dto.UserResponse;
import com.affiliate.rentals.gydi.users.application.mapper.UserDtoMapper;
import com.affiliate.rentals.gydi.users.domain.exception.InvalidRefreshTokenException;
import com.affiliate.rentals.gydi.users.domain.exception.UserNotFoundException;
import com.affiliate.rentals.gydi.users.domain.model.RefreshToken;
import com.affiliate.rentals.gydi.users.domain.model.User;
import com.affiliate.rentals.gydi.users.domain.ports.RefreshTokenRepository;
import com.affiliate.rentals.gydi.users.domain.ports.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for refreshing JWT access tokens using a refresh token.
 *
 * <p>This use case validates a refresh token and generates a new access token
 * if the refresh token is valid and not expired.</p>
 *
 * @author GYDI Development Team
 */
@Service
public class RefreshTokenUseCase {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final UserDtoMapper mapper;

    public RefreshTokenUseCase(
            RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository,
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
     * Executes the refresh token use case.
     *
     * @param request the refresh token request
     * @return an AuthResponse with a new access token
     * @throws InvalidRefreshTokenException if the refresh token is invalid or expired
     * @throws UserNotFoundException if the user associated with the token doesn't exist
     */
    @Transactional
    public AuthResponse execute(RefreshTokenRequest request) {
        // Find and validate the refresh token
        RefreshToken refreshToken = refreshTokenRepository
                .findByToken(request.refreshToken())
                .orElseThrow(() -> new InvalidRefreshTokenException("Invalid refresh token"));

        // Check if the refresh token is valid
        if (!refreshToken.isValid()) {
            if (refreshToken.isRevoked()) {
                throw new InvalidRefreshTokenException("Refresh token has been revoked");
            }
            throw new InvalidRefreshTokenException("Refresh token has expired");
        }

        // Find the user associated with the refresh token
        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> UserNotFoundException.withId(refreshToken.getUserId()));

        // Generate new access token with userId claim
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.email().address());
        String newAccessToken = jwtService.generateTokenWithUserId(userDetails, user.id());

        // Map user to response DTO
        UserResponse userResponse = mapper.toResponse(user);

        // Return response without a new refresh token (reuse existing)
        return AuthResponse.ofRefresh(newAccessToken, userResponse);
    }
}