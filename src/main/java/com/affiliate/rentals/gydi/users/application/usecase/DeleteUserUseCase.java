package com.affiliate.rentals.gydi.users.application.usecase;

import com.affiliate.rentals.gydi.users.domain.ports.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for deleting a user by their unique identifier.
 *
 * <p>This service implements the business logic for removing a user from the system.
 * It follows the hexagonal architecture pattern by depending on the UserRepository
 * port rather than concrete implementations.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * deleteUserUseCase.execute(1L);
 * }</pre>
 *
 * @author GYDI Development Team
 * @see UserRepository
 */
@Service
public class DeleteUserUseCase {

    private final UserRepository userRepository;

    /**
     * Constructs a new DeleteUserUseCase with required dependencies.
     *
     * @param userRepository the repository for user persistence operations
     */
    public DeleteUserUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Executes the delete user use case.
     *
     * <p>This method verifies the user exists before attempting deletion.
     * If the user doesn't exist, an exception is thrown.</p>
     *
     * @param id the unique identifier of the user to delete
     * @throws IllegalArgumentException if no user is found with the given ID
     */
    @Transactional
    public void execute(Long id) {
        if (!userRepository.findById(id).isPresent()) {
            throw new IllegalArgumentException("User not found with id: " + id);
        }

        userRepository.deleteById(id);
    }
}
