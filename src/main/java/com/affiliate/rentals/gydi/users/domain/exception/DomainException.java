package com.affiliate.rentals.gydi.users.domain.exception;

/**
 * Base exception for all domain-level exceptions in the users context.
 *
 * <p>This is a sealed class (Java 17+) that only allows specific domain exceptions
 * to extend it, providing compile-time guarantees about exception hierarchy and
 * enabling exhaustive pattern matching.</p>
 *
 * <p>Domain exceptions represent business rule violations and should be thrown
 * from the domain layer when invariants are broken or business logic fails.</p>
 *
 * @author GYDI Development Team
 */
public sealed abstract class DomainException extends RuntimeException
        permits UserNotFoundException,
                UserAlreadyExistsException,
                InvalidCredentialsException,
                InvalidUserDataException,
                InvalidRefreshTokenException,
                PermissionDeniedException {

    /**
     * Constructs a new domain exception with the specified detail message.
     *
     * @param message the detail message
     */
    protected DomainException(String message) {
        super(message);
    }

    /**
     * Constructs a new domain exception with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause of this exception
     */
    protected DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}