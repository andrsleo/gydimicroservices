package com.affiliate.rentals.gydi.shared.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SecurityException;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for generating and validating JWE (Encrypted JWT) referral tokens.
 *
 * <p>
 * <b>Security Features:</b>
 * </p>
 * <ul>
 * <li><b>Confidentiality:</b> Payload is encrypted using A256GCM
 * (AES-256-GCM).</li>
 * <li><b>Integrity:</b> GCM mode provides authenticated encryption
 * (tamper-proof).</li>
 * <li><b>Expiration:</b> Tokens expire after a configured duration (customizable per-link, default max 365 days).</li>
 * <li><b>Uniqueness:</b> Uses JTI (JWT ID) to prevent replay attacks if
 * tracked.</li>
 * </ul>
 *
 * <p>
 * <b>Token Structure (Encrypted):</b>
 * </p>
 * 
 * <pre>
 * Header: { "alg": "dir", "enc": "A256GCM" }
 * Payload: {
 *   "sub": "referrerUserId",
 *   "propertyId": "propertyId",
 *   "iat": timestamp,
 *   "exp": timestamp,
 *   "jti": "uuid"
 * }
 * </pre>
 *
 * @author GYDI Development Team
 */
@Service
@Slf4j
public class JwtReferralTokenService {

    private static final long DEFAULT_EXPIRATION_MS = 365L * 24 * 60 * 60 * 1000; // 365 days (max default)
    private static final String CLAIM_PROPERTY_ID = "propertyId";

    private final SecretKey encryptionKey;
    private final long expirationMs;

    public JwtReferralTokenService(
            @Value("${jwt.referral.encryption-secret:CHANGE_ME_JWT_REFERRAL_ENCRYPTION_SECRET_KEY_MUST_BE_32_BYTES}") String secret,
            @Value("${jwt.referral.expiration-ms:2592000000}") long expirationMs) {

        // Derive a 32-byte key from the secret using SHA-256
        // This ensures we always have a valid 256-bit key for AES-GCM, regardless of
        // input length.
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(secret.getBytes(StandardCharsets.UTF_8));

            // Use the derived 32-byte key for encryption (Direct Encryption with A256GCM)
            this.encryptionKey = new javax.crypto.spec.SecretKeySpec(keyBytes, "AES");
            this.expirationMs = expirationMs;

            log.info("JwtReferralTokenService (JWE) initialized with expiration: {} days",
                    expirationMs / (24 * 60 * 60 * 1000));

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Generates an encrypted JWE referral token with default expiration.
     *
     * @param userId     the ID of the user generating the referral link
     * @param propertyId the Long of the property being referred
     * @return encrypted JWE token string
     */
    public String generateReferralToken(Long userId, Long propertyId) {
        return generateReferralToken(userId, propertyId, this.expirationMs);
    }

    /**
     * Generates an encrypted JWE referral token with custom expiration.
     *
     * @param userId          the ID of the user generating the referral link
     * @param propertyId      the Long of the property being referred
     * @param customExpirationMs custom expiration duration in milliseconds
     * @return encrypted JWE token string
     */
    public String generateReferralToken(Long userId, Long propertyId, long customExpirationMs) {
        if (userId == null)
            throw new IllegalArgumentException("User ID cannot be null");
        if (propertyId == null)
            throw new IllegalArgumentException("Property ID cannot be null");
        if (customExpirationMs <= 0)
            throw new IllegalArgumentException("Expiration time must be positive");

        Date now = new Date();
        Date expiration = new Date(now.getTime() + customExpirationMs);
        String jti = UUID.randomUUID().toString();

        // Create JWE
        // We use "dir" (Direct Encryption) with the shared secret, and A256GCM for
        // content encryption.
        String token = Jwts.builder()
                .subject(userId.toString())
                .claim(CLAIM_PROPERTY_ID, propertyId.toString())
                .id(jti)
                .issuedAt(now)
                .expiration(expiration)
                .encryptWith(encryptionKey, Jwts.ENC.A256GCM) // Encrypts payload
                .compact();

        log.debug("Generated JWE referral token for userId={}, propertyId={}, jti={}, expirationDays={}",
                userId, propertyId, jti, customExpirationMs / (24 * 60 * 60 * 1000));

        return token;
    }

    /**
     * Decrypts and validates a referral token.
     *
     * @param token the JWE token to decrypt
     * @return extracted referral data
     * @throws InvalidReferralTokenException if invalid, expired, or tampered
     */
    public ReferralTokenData validateAndExtract(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new InvalidReferralTokenException("Referral token cannot be null or empty");
        }

        try {
            Claims claims = Jwts.parser()
                    .decryptWith(encryptionKey) // Decrypts using the shared secret
                    .build()
                    .parseEncryptedClaims(token)
                    .getPayload();

            // Extract userId
            String userIdStr = claims.getSubject();
            if (userIdStr == null || userIdStr.isBlank()) {
                throw new InvalidReferralTokenException("Token missing user ID");
            }
            Long userId = Long.parseLong(userIdStr);

            // Extract propertyId
            String propertyIdStr = claims.get(CLAIM_PROPERTY_ID, String.class);
            if (propertyIdStr == null || propertyIdStr.isBlank()) {
                throw new InvalidReferralTokenException("Token missing property ID");
            }
            Long propertyId = Long.parseLong(propertyIdStr);

            log.debug("Successfully decrypted referral token: userId={}, propertyId={}", userId, propertyId);
            return new ReferralTokenData(userId, propertyId);

        } catch (ExpiredJwtException e) {
            log.warn("SECURITY: Expired referral token used: {}", e.getMessage());
            throw new InvalidReferralTokenException("Referral link has expired", e);
        } catch (SecurityException | MalformedJwtException e) {
            // SecurityException covers decryption failures (wrong key, tampered ciphertext)
            log.warn("SECURITY: Failed to decrypt/verify referral token: {}", e.getMessage());
            throw new InvalidReferralTokenException("Invalid or tampered referral token", e);
        } catch (NumberFormatException e) {
            throw new InvalidReferralTokenException("Invalid ID format in token claims", e);
        } catch (Exception e) {
            log.error("Unexpected error validating referral token", e);
            throw new InvalidReferralTokenException("Failed to validate referral token", e);
        }
    }

    public boolean isValidToken(String token) {
        try {
            validateAndExtract(token);
            return true;
        } catch (InvalidReferralTokenException e) {
            return false;
        }
    }

    public record ReferralTokenData(Long userId, Long propertyId) {
    }

    public static class InvalidReferralTokenException extends RuntimeException {
        public InvalidReferralTokenException(String message) {
            super(message);
        }

        public InvalidReferralTokenException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}