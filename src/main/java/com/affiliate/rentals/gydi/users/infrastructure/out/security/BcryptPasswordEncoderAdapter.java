package com.affiliate.rentals.gydi.users.infrastructure.out.security;

import com.affiliate.rentals.gydi.users.domain.service.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Adapter implementation of the PasswordEncoder port using BCrypt.
 *
 * <p>This adapter implements the hexagonal architecture's port interface,
 * bridging the domain layer with Spring Security's BCryptPasswordEncoder.
 * It provides secure password hashing and verification using the BCrypt
 * algorithm, which is designed to be computationally expensive to resist
 * brute-force attacks.</p>
 *
 * <p>BCrypt automatically handles salt generation and incorporates a
 * configurable work factor (cost) to adapt to increasing computational
 * power over time.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * PasswordEncoder encoder = new BcryptPasswordEncoderAdapter();
 * String hash = encoder.encode("myPassword");
 * boolean matches = encoder.matches("myPassword", hash); // true
 * }</pre>
 *
 * @author GYDI Development Team
 * @see PasswordEncoder
 * @see BCryptPasswordEncoder
 */
@Component
public class BcryptPasswordEncoderAdapter implements PasswordEncoder {

    private final BCryptPasswordEncoder bcryptEncoder;

    /**
     * Constructs a new BcryptPasswordEncoderAdapter with default BCrypt strength.
     *
     * <p>The default strength is 10, which provides a good balance between
     * security and performance for most applications.</p>
     */
    public BcryptPasswordEncoderAdapter() {
        this.bcryptEncoder = new BCryptPasswordEncoder();
    }

    /**
     * Constructs a new BcryptPasswordEncoderAdapter with custom BCrypt strength.
     *
     * @param strength the log rounds to use, between 4 and 31
     */
    public BcryptPasswordEncoderAdapter(int strength) {
        this.bcryptEncoder = new BCryptPasswordEncoder(strength);
    }

    @Override
    public String encode(String rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("Raw password cannot be null");
        }
        return bcryptEncoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("Raw password cannot be null");
        }
        if (encodedPassword == null) {
            throw new IllegalArgumentException("Encoded password cannot be null");
        }
        return bcryptEncoder.matches(rawPassword, encodedPassword);
    }
}
