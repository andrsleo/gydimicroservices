package com.affiliate.rentals.gydi.users.domain.ports;

/**
 * Port interface for password encoding operations.
 *
 * <p>This interface abstracts password encoding/hashing operations from the domain layer,
 * following the hexagonal architecture pattern. The actual implementation will be provided
 * by the infrastructure layer using Spring Security's BCrypt or similar.</p>
 *
 * @author GYDI Development Team
 */
public interface PasswordEncoderPort {

    /**
     * Encodes a raw password into a hashed format.
     *
     * @param rawPassword the plain text password
     * @return the encoded (hashed) password
     */
    String encode(String rawPassword);

    /**
     * Verifies if a raw password matches an encoded password.
     *
     * @param rawPassword the plain text password to verify
     * @param encodedPassword the encoded password to compare against
     * @return true if the passwords match, false otherwise
     */
    boolean matches(String rawPassword, String encodedPassword);
}