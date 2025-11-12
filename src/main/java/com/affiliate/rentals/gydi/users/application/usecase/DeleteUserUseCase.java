package com.affiliate.rentals.gydi.users.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.affiliate.rentals.gydi.shared.security.OwnershipValidator;
import com.affiliate.rentals.gydi.users.domain.exception.UserNotFoundException;
import com.affiliate.rentals.gydi.users.domain.ports.UserRepositoryPort;

/**
 * Use case for deleting a user by their unique identifier.
 *
 * <p>This service implements the business logic for removing a user from the system.
 * It follows the hexagonal architecture pattern by depending on the UserRepository
 * port rather than concrete implementations.</p>
 *
 * <p><b>SECURITY: IDOR Prevention</b> - Validates that users can only delete their own account.</p>
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

    private final UserRepositoryPort userRepository;
    private final OwnershipValidator ownershipValidator;

    /**
     * Constructs a new DeleteUserUseCase with required dependencies.
     *
     * @param userRepository the repository for user persistence operations
     * @param ownershipValidator the validator for ownership verification
     */
    public DeleteUserUseCase(
            UserRepositoryPort userRepository,
            OwnershipValidator ownershipValidator
    ) {
        this.userRepository = userRepository;
        this.ownershipValidator = ownershipValidator;
    }

    /**
     * Executes the delete user use case.
     *
     * <p>This method verifies the user exists before attempting deletion.
     * If the user doesn't exist, an exception is thrown.</p>
     *
     * @param id the unique identifier of the user to delete
     * @throws UserNotFoundException if no user is found with the given ID
     * @throws com.affiliate.rentals.gydi.shared.exception.ForbiddenException if user doesn't own the account
     */
    @Transactional
    public void execute(Long id) {
        // SECURITY: Validate ownership to prevent IDOR
        ownershipValidator.validateOwnership(id);

        if (!userRepository.findById(id).isPresent()) {
            throw UserNotFoundException.withId(id);
        }

        userRepository.deleteById(id);
    }
}
