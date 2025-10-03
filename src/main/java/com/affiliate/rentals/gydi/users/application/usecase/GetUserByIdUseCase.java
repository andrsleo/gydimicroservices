package com.affiliate.rentals.gydi.users.application.usecase;

import com.affiliate.rentals.gydi.users.application.dto.UserResponse;
import com.affiliate.rentals.gydi.users.application.mapper.UserDtoMapper;
import com.affiliate.rentals.gydi.users.domain.model.User;
import com.affiliate.rentals.gydi.users.domain.ports.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for retrieving a user by their unique identifier.
 *
 * <p>This service implements the business logic for finding a specific user
 * by ID. It follows the hexagonal architecture pattern by depending on the
 * UserRepository port rather than concrete implementations.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * UserResponse user = getUserByIdUseCase.execute(1L);
 * }</pre>
 *
 * @author GYDI Development Team
 * @see User
 * @see UserRepository
 * @see UserDtoMapper
 */
@Service
public class GetUserByIdUseCase {

    private final UserRepository userRepository;
    private final UserDtoMapper mapper;

    /**
     * Constructs a new GetUserByIdUseCase with required dependencies.
     *
     * @param userRepository the repository for user persistence operations
     * @param mapper the mapper for converting between domain andDTO objects
     */
    public GetUserByIdUseCase(UserRepository userRepository, UserDtoMapper mapper) {
        this.userRepository = userRepository;
        this.mapper = mapper;
    }

    /**
     * Executes the get user by ID use case.
     *
     * @param id the unique identifier of the user to retrieve
     * @return the user as a UserResponse DTO
     * @throws IllegalArgumentException if no user is found with the given ID
     */
    @Transactional(readOnly = true)
    public UserResponse execute(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));

        return mapper.toResponse(user);
    }
}
