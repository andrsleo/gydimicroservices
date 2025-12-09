package com.affiliate.rentals.gydi.shared.security;

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
 * 🔒 JPA Attribute Converter for encrypting/decrypting iCal URLs
 *
 * Why encrypt iCal URLs?
 * - URLs contain access tokens (e.g., https://airbnb.com/calendar.ics?token=SECRET_TOKEN)
 * - Tokens grant read access to calendar data
 * - Database breach would expose all tokens
 *
 * Algorithm: AES-256-GCM
 * - Authenticated encryption (prevents tampering)
 * - Random IV per encryption (prevents pattern analysis)
 * - AEAD (Authenticated Encryption with Associated Data)
 *
 * Usage:
 * @Column(columnDefinition = "TEXT")
 * @Convert(converter = ICalUrlEncryptor.class)
 * private String iCalUrl;
 */
@Slf4j
@Component
@Converter
public class ICalUrlEncryptor implements AttributeConverter<String, String> {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;  // 96 bits (recommended for GCM)
    private static final int GCM_TAG_LENGTH = 128; // 128 bits authentication tag
    private static final int AES_KEY_SIZE = 256;   // 256 bits

    // SECURITY: AES secret MUST be set via environment variable
    // Generate with: openssl rand -hex 32
    // DO NOT commit actual secrets to git
    @Value("${security.url.aes.secret}")
    private String encryptionSecret;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Encrypts URL before storing in database
     *
     * @param plainUrl Plain text URL
     * @return Base64-encoded: [IV (12 bytes) + Encrypted data + Auth tag (16 bytes)]
     */
    @Override
    public String convertToDatabaseColumn(String plainUrl) {
        if (plainUrl == null || plainUrl.isBlank()) {
            return null;
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
            byte[] encrypted = cipher.doFinal(plainUrl.getBytes("UTF-8"));

            // ✅ Combine IV + encrypted data
            byte[] combined = ByteBuffer.allocate(iv.length + encrypted.length)
                .put(iv)
                .put(encrypted)
                .array();

            // ✅ Encode to Base64 for database storage
            String encoded = Base64.getEncoder().encodeToString(combined);

            log.debug("Encrypted iCal URL (length: {})", plainUrl.length());
            return encoded;

        } catch (Exception e) {
            log.error("🚨 Failed to encrypt iCal URL: {}", e.getMessage());
            throw new EncryptionException("Failed to encrypt iCal URL", e);
        }
    }

    /**
     * Decrypts URL when reading from database
     *
     * @param encryptedUrl Base64-encoded encrypted URL
     * @return Plain text URL
     */
    @Override
    public String convertToEntityAttribute(String encryptedUrl) {
        if (encryptedUrl == null || encryptedUrl.isBlank()) {
            return null;
        }

        try {
            // ✅ Decode from Base64
            byte[] combined = Base64.getDecoder().decode(encryptedUrl);

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

            String plainUrl = new String(decrypted, "UTF-8");
            log.debug("Decrypted iCal URL (length: {})", plainUrl.length());

            return plainUrl;

        } catch (Exception e) {
            log.error("🚨 Failed to decrypt iCal URL: {}", e.getMessage());
            throw new EncryptionException("Failed to decrypt iCal URL", e);
        }
    }

    /**
     * Derives AES key from encryption secret
     *
     * SECURITY NOTE:
     * - In production (Fly.io), set via: fly secrets set AES_SECRET_KEY="$(openssl rand -hex 32)"
     * - Key must be exactly 64 hex characters (32 bytes = 256 bits)
     * - Never commit actual keys to git
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
            throw new EncryptionException("Failed to derive encryption key", e);
        }
    }

    /**
     * Converts hex string to byte array
     *
     * @param hexString Hex string (e.g., "a1b2c3d4...")
     * @return Byte array
     */
    private byte[] hexStringToByteArray(String hexString) {
        int len = hexString.length();
        if (len % 2 != 0) {
            throw new IllegalArgumentException("Hex string must have even length");
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
