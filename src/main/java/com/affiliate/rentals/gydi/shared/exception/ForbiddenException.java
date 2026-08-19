package com.affiliate.rentals.gydi.shared.exception;

/**
 * Exception thrown when a user attempts to access a resource they don't own.
 *
 * <p><b>SECURITY: IDOR (Insecure Direct Object Reference) Prevention</b></p>
 *
 * <p>This exception is thrown when a user tries to access, modify, or delete
 * a resource that belongs to another user without proper authorization.</p>
 *
 * <p>Example scenarios:</p>
 * <ul>
 *   <li>User A tries to view User B's profile</li>
 *   <li>User A tries to modify User B's data</li>
 *   <li>User A tries to delete User B's resources</li>
 * </ul>
 *
 * <p>This exception should result in HTTP 403 Forbidden response.</p>
 *
 * @author GYDI Development Team
 */
public class ForbiddenException extends RuntimeException {

    /**
     * Constructs a new ForbiddenException with the specified detail message.
     *
     * @param message the detail message
     */
    public ForbiddenException(String message) {
        super(message);
    }

    /**
     * Constructs a new ForbiddenException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public ForbiddenException(String message, Throwable cause) {
        super(message, cause);
    }
}