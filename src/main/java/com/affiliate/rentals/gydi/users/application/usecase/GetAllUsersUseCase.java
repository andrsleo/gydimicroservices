package com.affiliate.rentals.gydi.users.application.usecase;

import com.affiliate.rentals.gydi.users.application.dto.UserResponse;
import com.affiliate.rentals.gydi.users.application.mapper.UserDtoMapper;
import com.affiliate.rentals.gydi.users.domain.ports.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Use case for retrieving all users in the system.
 *
 * <p>This service implements the business logic for fetching all users.
 * It follows the hexagonal architecture pattern by depending on the
 * UserRepository port rather than concrete implementations.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * List<UserResponse> users = getAllUsersUseCase.execute();
 * }</pre>
 *
 * @author GYDI Development Team
 * @see UserRepository
 * @see UserDtoMapper
 */
@Service
public class GetAllUsersUseCase {

    private final UserRepository userRepository;
    private final UserDtoMapper mapper;

    /**
     * Constructs a new GetAllUsersUseCase with required dependencies.
     *
     * @param userRepository the repository for user persistence operations
     * @param mapper the mapper for converting between domain and DTO objects
     */
    public GetAllUsersUseCase(UserRepository userRepository, UserDtoMapper mapper) {
        this.userRepository = userRepository;
        this.mapper = mapper;
    }

    /**
     * Executes the get all users use case.
     *
     * @return a list of all users as UserResponse DTOs
     */
    @Transactional(readOnly = true)
    public List<UserResponse> execute() {
        return userRepository.findAll()
                .stream()
                .map(mapper::toResponse)
                .toList();
    }
}
