package com.affiliate.rentals.gydi.shared.security;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.hashids.Hashids;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for encrypting and decrypting data in URLs.
 *
 * <p>
 * <b>Security Strategy:</b>
 * </p>
 * <ul>
 * <li><b>Property IDs (Longs):</b> Hashids obfuscation (deterministic,
 * cacheable, URL-safe)</li>
 * <li><b>User IDs (referral):</b> AES-256-GCM encryption (secure, with
 * timestamp for replay protection)</li>
 * </ul>
 *
 * <p>
 * <b>Example Usage:</b>
 * </p>
 * 
 * <pre>{@code
 * // Encrypt property ID
 * Long propertyId = Long.fromString("c5e4573e-5e69-4a49-bfbd-84385ddc3c70");
 * String encoded = urlEncryptionService.encodePropertyId(propertyId);
 * // Result: "x7k2mP9qL" (short, URL-safe, deterministic)
 *
 * // Decrypt property ID
 * Long decoded = urlEncryptionService.decodePropertyId("x7k2mP9qL");
 *
 * // Encrypt user ID for referral
 * Long userId = 12345L;
 * String encrypted = urlEncryptionService.encryptUserId(userId);
 * // Result: "a8F3nQ..." (encrypted with AES-GCM + timestamp)
 *
 * // Decrypt user ID
 * Long decrypted = urlEncryptionService.decryptUserId("a8F3nQ...");
 * }</pre>
 *
 * @author GYDI Development Team
 */
@Service
@Slf4j
public class UrlEncryptionService {

    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final long MAX_TOKEN_AGE_MS = 30L * 24 * 60 * 60 * 1000; // 30 days

    private final Hashids propertyHashids;
    private final SecretKey aesKey;
    private final SecureRandom secureRandom;

    public UrlEncryptionService(
            @Value("${security.url.hashids.salt:gydi-property-salt-change-me}") String hashidsSalt,
            @Value("${security.url.hashids.min-length:8}") int hashidsMinLength,
            @Value("${security.url.aes.secret:CHANGE_ME_32_CHAR_SECRET_KEY_HERE}") String aesSecret) {

        // Initialize Hashids for property ID obfuscation
        this.propertyHashids = new Hashids(hashidsSalt, hashidsMinLength);

        // Initialize AES key for user ID encryption
        byte[] keyBytes = aesSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length != 32) {
            log.warn(
                    "SECURITY WARNING: AES key must be exactly 32 bytes (256 bits). Current length: {}. Using padded/truncated key.",
                    keyBytes.length);
            byte[] paddedKey = new byte[32];
            System.arraycopy(keyBytes, 0, paddedKey, 0, Math.min(keyBytes.length, 32));
            keyBytes = paddedKey;
        }
        this.aesKey = new SecretKeySpec(keyBytes, "AES");
        this.secureRandom = new SecureRandom();

        log.info("UrlEncryptionService initialized with Hashids min length: {} and AES-256-GCM", hashidsMinLength);
    }

    /**
     * Encodes a property Long into a short, URL-safe string using Hashids.
     *
     * <p>
     * This method is deterministic: same Long always produces same encoded string.
     * This is important for CDN caching and SEO.
     * </p>
     *
     * @param propertyId the property Long to encode
     * @return URL-safe encoded string (e.g., "x7k2mP9qL")
     * @throws IllegalArgumentException if propertyId is null
     */
    public String encodePropertyId(Long propertyId) {
        if (propertyId == null) {
            throw new IllegalArgumentException("Property ID cannot be null");
        }

        // Convert Long to two longs (most significant bits and least significant bits)
        long mostSigBits = propertyId;
        long leastSigBits = propertyId;

        // Encode as positive longs (Hashids requires positive numbers)
        long[] numbers = {
                Long.divideUnsigned(mostSigBits, 1) & Long.MAX_VALUE,
                Long.divideUnsigned(leastSigBits, 1) & Long.MAX_VALUE
        };

        return propertyHashids.encode(numbers);
    }

    /**
     * Decodes a Hashids-encoded string back to the original Long.
     *
     * @param encoded the encoded string
     * @return the original Long
     * @throws IllegalArgumentException if encoded string is invalid
     */
    public UUID decodePropertyId(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            throw new IllegalArgumentException("Encoded property ID cannot be null or empty");
        }

        try {
            long[] decoded = propertyHashids.decode(encoded);
            if (decoded.length != 2) {
                throw new IllegalArgumentException("Invalid encoded property ID format");
            }

            long mostSigBits = decoded[0];
            long leastSigBits = decoded[1];

            return new UUID(mostSigBits, leastSigBits);
        } catch (Exception e) {
            log.error("Failed to decode property ID: {}", encoded, e);
            throw new IllegalArgumentException("Invalid encoded property ID", e);
        }
    }

    /**
     * Encrypts a user ID using AES-256-GCM with timestamp for replay protection.
     *
     * <p>
     * Format: Base64URL(IV + encrypted(userId + timestamp))
     * </p>
     *
     * @param userId the user ID to encrypt
     * @return encrypted, URL-safe string
     * @throws IllegalArgumentException if userId is null
     * @throws RuntimeException         if encryption fails
     */
    public String encryptUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        try {
            // Generate random IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            // Prepare data: userId (8 bytes) + timestamp (8 bytes)
            long timestamp = System.currentTimeMillis();
            ByteBuffer buffer = ByteBuffer.allocate(16);
            buffer.putLong(userId);
            buffer.putLong(timestamp);
            byte[] plaintext = buffer.array();

            // Encrypt using AES-GCM
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, parameterSpec);
            byte[] ciphertext = cipher.doFinal(plaintext);

            // Combine IV + ciphertext
            ByteBuffer combined = ByteBuffer.allocate(iv.length + ciphertext.length);
            combined.put(iv);
            combined.put(ciphertext);

            // Encode as Base64URL (URL-safe, no padding)
            return Base64.getUrlEncoder().withoutPadding().encodeToString(combined.array());

        } catch (Exception e) {
            log.error("Failed to encrypt user ID: {}", userId, e);
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * Decrypts a user ID encrypted with {@link #encryptUserId(Long)}.
     *
     * <p>
     * Validates timestamp to prevent replay attacks (max age: 30 days).
     * </p>
     *
     * @param encrypted the encrypted string
     * @return the original user ID
     * @throws IllegalArgumentException if encrypted string is invalid or expired
     * @throws RuntimeException         if decryption fails
     */
    public Long decryptUserId(String encrypted) {
        if (encrypted == null || encrypted.isEmpty()) {
            throw new IllegalArgumentException("Encrypted user ID cannot be null or empty");
        }

        try {
            // Decode from Base64URL
            byte[] combined = Base64.getUrlDecoder().decode(encrypted);

            if (combined.length < GCM_IV_LENGTH) {
                throw new IllegalArgumentException("Invalid encrypted data length");
            }

            // Extract IV and ciphertext
            ByteBuffer buffer = ByteBuffer.wrap(combined);
            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);

            // Decrypt using AES-GCM
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, aesKey, parameterSpec);
            byte[] plaintext = cipher.doFinal(ciphertext);

            // Extract userId and timestamp
            ByteBuffer dataBuffer = ByteBuffer.wrap(plaintext);
            long userId = dataBuffer.getLong();
            long timestamp = dataBuffer.getLong();

            // Validate timestamp (prevent replay attacks with old links)
            long age = System.currentTimeMillis() - timestamp;
            if (age > MAX_TOKEN_AGE_MS) {
                log.warn("SECURITY: Expired referral link used (age: {} days)", age / (24 * 60 * 60 * 1000));
                throw new IllegalArgumentException("Referral link has expired");
            }
            if (age < 0) {
                log.warn("SECURITY: Referral link with future timestamp detected");
                throw new IllegalArgumentException("Invalid referral link timestamp");
            }

            return userId;

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to decrypt user ID", e);
            throw new RuntimeException("Decryption failed", e);
        }
    }

    /**
     * Validates if an encrypted user ID is still valid (not expired).
     *
     * @param encrypted the encrypted user ID
     * @return true if valid, false otherwise
     */
    public boolean isValidReferralToken(String encrypted) {
        try {
            decryptUserId(encrypted);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}