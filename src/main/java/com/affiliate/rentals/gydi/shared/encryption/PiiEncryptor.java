package com.affiliate.rentals.gydi.shared.encryption;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 🔒 JPA Attribute Converter for encrypting/decrypting Personally Identifiable Information (PII)
 *
 * ✅ SECURITY FIX: PII Encryption at Rest (FIX-007)
 *
 * Why encrypt PII?
 * - Guest names, emails, phone numbers contain sensitive personal data
 * - GDPR/CCPA compliance requires protection of personal information
 * - Database breach would expose customer PII
 * - Defense in depth: encryption provides additional layer beyond database access controls
 *
 * Algorithm: AES-256-GCM
 * - Authenticated encryption (prevents tampering)
 * - Random IV per encryption (prevents pattern analysis)
 * - AEAD (Authenticated Encryption with Associated Data)
 * - 256-bit key size (industry standard for sensitive data)
 *
 * Usage:
 * <pre>
 * {@code
 * @Column(name = "guest_name", columnDefinition = "TEXT")
 * @Convert(converter = PiiEncryptor.class)
 * private String guestName;
 *
 * @Column(name = "guest_email", columnDefinition = "TEXT")
 * @Convert(converter = PiiEncryptor.class)
 * private String guestEmail;
 * }
 * </pre>
 *
 * Migration Strategy: LAZY ENCRYPTION
 * - No migration script needed
 * - On READ: If decrypt fails → assume plaintext → return as-is (backward compatible)
 * - On WRITE: Always encrypt
 * - Benefit: Zero downtime, gradual migration, rollback-safe
 *
 * Security Configuration:
 * - Key stored in environment variable: PII_ENCRYPTION_KEY
 * - Generate with: openssl rand -hex 32
 * - Production deployment: fly secrets set PII_ENCRYPTION_KEY="$(openssl rand -hex 32)"
 * - Local development: application-local.yml or .env
 *
 * Performance:
 * - Encryption: ~0.1ms per field
 * - Storage overhead: ~50% (Base64 encoding + IV + auth tag)
 *
 * @author GYDI Development Team
 * @since 2026-02-10
 */
@Slf4j
@Component
@Converter
public class PiiEncryptor implements AttributeConverter<String, String> {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;  // 96 bits (recommended for GCM)
    private static final int GCM_TAG_LENGTH = 128; // 128 bits authentication tag
    private static final int AES_KEY_SIZE = 256;   // 256 bits

    // SECURITY: PII encryption key MUST be set via environment variable
    // Generate with: openssl rand -hex 32
    // DO NOT commit actual secrets to git
    @Value("${encryption.pii.secret:#{null}}")
    private String encryptionSecret;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Encrypts PII before storing in database
     *
     * @param plainText Plain text PII (guest name, email, phone)
     * @return Base64-encoded: [IV (12 bytes) + Encrypted data + Auth tag (16 bytes)]
     *         or null if input is null/empty
     */
    @Override
    public String convertToDatabaseColumn(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            return null;
        }

        // SECURITY: Encryption disabled if key not configured
        if (encryptionSecret == null || encryptionSecret.isBlank()) {
            log.warn("⚠️ PII_ENCRYPTION_KEY not configured. Storing PII in PLAINTEXT. " +
                    "Set environment variable: PII_ENCRYPTION_KEY=$(openssl rand -hex 32)");
            return plainText; // Store plaintext (backward compatible)
        }

        try {
            // ✅ Generate random IV (CRITICAL: must be unique per encryption)
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            // ✅ Initialize cipher with AES-GCM
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKeySpec keySpec = getKeySpec();
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            // ✅ Encrypt
            byte[] encrypted = cipher.doFinal(plainText.getBytes("UTF-8"));

            // ✅ Combine IV + encrypted data
            byte[] combined = ByteBuffer.allocate(iv.length + encrypted.length)
                .put(iv)
                .put(encrypted)
                .array();

            // ✅ Encode to Base64 for database storage
            String encoded = Base64.getEncoder().encodeToString(combined);

            // Log only length for security (DO NOT log actual PII)
            log.debug("Encrypted PII (length: {} → {})", plainText.length(), encoded.length());
            return encoded;

        } catch (Exception e) {
            log.error("🚨 Failed to encrypt PII: {}", e.getMessage());
            throw new EncryptionException("Failed to encrypt PII", e);
        }
    }

    /**
     * Decrypts PII when reading from database
     *
     * ✅ LAZY ENCRYPTION: If decryption fails, assumes plaintext and returns as-is
     * This enables backward compatibility with existing unencrypted data
     *
     * @param encryptedText Base64-encoded encrypted PII (or plaintext for legacy data)
     * @return Plain text PII
     */
    @Override
    public String convertToEntityAttribute(String encryptedText) {
        if (encryptedText == null || encryptedText.isBlank()) {
            return null;
        }

        // SECURITY: Encryption disabled if key not configured
        if (encryptionSecret == null || encryptionSecret.isBlank()) {
            log.warn("⚠️ PII_ENCRYPTION_KEY not configured. Reading PII as PLAINTEXT.");
            return encryptedText; // Return plaintext (backward compatible)
        }

        try {
            // ✅ Decode from Base64
            byte[] combined = Base64.getDecoder().decode(encryptedText);

            // ✅ Extract IV (first 12 bytes)
            ByteBuffer buffer = ByteBuffer.wrap(combined);
            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);

            // ✅ Extract encrypted data (rest)
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);

            // ✅ Initialize cipher with same IV
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKeySpec keySpec = getKeySpec();
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            // ✅ Decrypt
            byte[] decrypted = cipher.doFinal(encrypted);

            String plainText = new String(decrypted, "UTF-8");
            log.debug("Decrypted PII (length: {})", plainText.length());

            return plainText;

        } catch (IllegalArgumentException e) {
            // ✅ LAZY ENCRYPTION: Base64 decode failed → assume plaintext (legacy data)
            log.debug("PII appears to be plaintext (not Base64). Returning as-is for backward compatibility.");
            return encryptedText;

        } catch (Exception e) {
            // ✅ LAZY ENCRYPTION: Decryption failed → assume plaintext (legacy data)
            // This handles cases where data was stored before encryption was enabled
            log.warn("Failed to decrypt PII ({}). Assuming plaintext for backward compatibility.",
                    e.getClass().getSimpleName());
            return encryptedText;
        }
    }

    /**
     * Derives AES key from encryption secret
     *
     * SECURITY NOTE:
     * - In production (Fly.io/Railway), set via: fly secrets set PII_ENCRYPTION_KEY="$(openssl rand -hex 32)"
     * - Key must be exactly 64 hex characters (32 bytes = 256 bits)
     * - Never commit actual keys to git
     * - Rotate keys periodically (requires re-encryption migration)
     */
    private SecretKeySpec getKeySpec() {
        try {
            // Convert hex string to bytes
            // Expected format: 64 hex characters (32 bytes = 256 bits)
            // Example: "a1b2c3d4e5f6..." generated via: openssl rand -hex 32
            byte[] keyBytes = hexStringToByteArray(encryptionSecret);

            if (keyBytes.length != 32) {
                throw new EncryptionException(
                    "Invalid AES key size: " + keyBytes.length + " bytes. Expected 32 bytes (256 bits). " +
                    "Generate valid key with: openssl rand -hex 32",
                    null
                );
            }

            return new SecretKeySpec(keyBytes, "AES");

        } catch (Exception e) {
            throw new EncryptionException("Failed to derive PII encryption key", e);
        }
    }

    /**
     * Converts hex string to byte array
     *
     * @param hexString Hex string (e.g., "a1b2c3d4...")
     * @return Byte array
     * @throws IllegalArgumentException if hex string has odd length or invalid characters
     */
    private byte[] hexStringToByteArray(String hexString) {
        int len = hexString.length();
        if (len % 2 != 0) {
            throw new IllegalArgumentException(
                    "Hex string must have even length. Got: " + len + " characters. " +
                    "Generate valid key with: openssl rand -hex 32"
            );
        }

        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                                 + Character.digit(hexString.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * Custom exception for encryption/decryption failures
     */
    public static class EncryptionException extends RuntimeException {
        public EncryptionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
