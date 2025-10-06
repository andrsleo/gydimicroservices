package com.affiliate.rentals.gydi.users.application.usecase;

import com.affiliate.rentals.gydi.shared.security.JwtService;
import com.affiliate.rentals.gydi.users.application.dto.AuthResponse;
import com.affiliate.rentals.gydi.users.application.dto.LoginRequest;
import com.affiliate.rentals.gydi.users.application.dto.UserResponse;
import com.affiliate.rentals.gydi.users.application.mapper.UserDtoMapper;
import com.affiliate.rentals.gydi.users.domain.exception.InvalidCredentialsException;
import com.affiliate.rentals.gydi.users.domain.model.Email;
import com.affiliate.rentals.gydi.users.domain.model.RefreshToken;
import com.affiliate.rentals.gydi.users.domain.model.User;
import com.affiliate.rentals.gydi.users.domain.ports.RefreshTokenRepository;
import com.affiliate.rentals.gydi.users.domain.ports.UserRepository;
import com.affiliate.rentals.gydi.users.domain.service.PasswordEncoder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for authenticating a user with email and password.
 *
 * <p>This service implements the business logic for user authentication.
 * It verifies the user's credentials by checking the email exists and
 * validating the password against the stored hash. Upon successful authentication,
 * it returns an AuthResponse with user information and a token placeholder.</p>
 *
 * <p>The actual JWT token generation will be handled by the security configuration
 * layer. This use case focuses on credential validation.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * LoginRequest request = new LoginRequest("user@example.com", "password");
 * AuthResponse response = authenticateUserUseCase.execute(request);
 * }</pre>
 *
 * @author GYDI Development Team
 * @see LoginRequest
 * @see AuthResponse
 * @see UserRepository
 * @see PasswordEncoder
 */
@Service
public class AuthenticateUserUseCase {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserDtoMapper mapper;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    /**
     * Constructs a new AuthenticateUserUseCase with required dependencies.
     *
     * @param userRepository the repository for user persistence operations
     * @param refreshTokenRepository the repository for refresh token operations
     * @param passwordEncoder the service for password encoding and validation
     * @param mapper the mapper for converting between domain and DTO objects
     * @param jwtService the service for JWT token generation
     * @param userDetailsService the service for loading user details
     */
    public AuthenticateUserUseCase(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            UserDtoMapper mapper,
            JwtService jwtService,
            UserDetailsService userDetailsService
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.mapper = mapper;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Executes the user authentication use case.
     *
     * <p>This method performs the following steps:</p>
     * <ol>
     *   <li>Validates the email format and finds the user</li>
     *   <li>Verifies the provided password against the stored hash</li>
     *   <li>Generates JWT access token and refresh token</li>
     *   <li>Returns AuthResponse with both tokens</li>
     * </ol>
     *
     * @param request the login request containing email and password
     * @return an AuthResponse with user information, access token, and refresh token
     * @throws InvalidCredentialsException if the email doesn't exist or password is invalid
     */
    @Transactional
    public AuthResponse execute(LoginRequest request) {
        Email email = Email.of(request.email());

        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
            throw new InvalidCredentialsException();
        }

        UserResponse userResponse = mapper.toResponse(user);

        // Generate JWT access token with userId claim for authenticated user
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.email());
        String accessToken = jwtService.generateTokenWithUserId(userDetails, user.id());

        // Create and save refresh token
        RefreshToken refreshToken = RefreshToken.create(user.id(), jwtService.getRefreshExpiration());
        refreshToken = refreshTokenRepository.save(refreshToken);

        return AuthResponse.of(accessToken, refreshToken.getToken(), userResponse);
    }
}
