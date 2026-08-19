package com.affiliate.rentals.gydi.shared.encryption;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PiiEncryptor
 *
 * ✅ SECURITY FIX (FIX-007): PII Encryption at Rest - Test Coverage
 *
 * Test Coverage:
 * - Roundtrip encryption/decryption
 * - Null/empty value handling
 * - Special characters and long strings
 * - Backward compatibility (plaintext data)
 * - Missing encryption key scenario
 */
@DisplayName("PII Encryptor Tests")
class PiiEncryptorTest {

    private PiiEncryptor encryptor;

    // Test encryption key (32 bytes = 64 hex chars)
    private static final String TEST_KEY = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    @BeforeEach
    void setUp() {
        encryptor = new PiiEncryptor();
        // Inject test key using reflection (simulates @Value injection)
        ReflectionTestUtils.setField(encryptor, "encryptionSecret", TEST_KEY);
    }

    @Test
    @DisplayName("Should encrypt and decrypt simple text successfully")
    void testSimpleEncryptDecrypt() {
        // Given
        String plainText = "John Doe";

        // When
        String encrypted = encryptor.convertToDatabaseColumn(plainText);
        String decrypted = encryptor.convertToEntityAttribute(encrypted);

        // Then
        assertNotNull(encrypted, "Encrypted text should not be null");
        assertNotEquals(plainText, encrypted, "Encrypted text should differ from plain text");
        assertEquals(plainText, decrypted, "Decrypted text should match original");
    }

    @Test
    @DisplayName("Should handle email addresses correctly")
    void testEmailEncryption() {
        // Given
        String email = "john.doe+test@example.com";

        // When
        String encrypted = encryptor.convertToDatabaseColumn(email);
        String decrypted = encryptor.convertToEntityAttribute(encrypted);

        // Then
        assertEquals(email, decrypted, "Email should decrypt correctly");
        assertTrue(encrypted.length() > email.length(), "Encrypted email should be longer than plain text");
    }

    @Test
    @DisplayName("Should handle phone numbers with special characters")
    void testPhoneNumberEncryption() {
        // Given
        String phone = "+1 (555) 123-4567";

        // When
        String encrypted = encryptor.convertToDatabaseColumn(phone);
        String decrypted = encryptor.convertToEntityAttribute(encrypted);

        // Then
        assertEquals(phone, decrypted, "Phone number should decrypt correctly");
    }

    @Test
    @DisplayName("Should handle long strings")
    void testLongStringEncryption() {
        // Given
        String longName = "Dr. Christopher Alexander Montgomery-Weatherington III";

        // When
        String encrypted = encryptor.convertToDatabaseColumn(longName);
        String decrypted = encryptor.convertToEntityAttribute(encrypted);

        // Then
        assertEquals(longName, decrypted, "Long string should decrypt correctly");
    }

    @Test
    @DisplayName("Should handle special characters and unicode")
    void testSpecialCharacters() {
        // Given
        String specialChars = "José García-López 日本語 🎉";

        // When
        String encrypted = encryptor.convertToDatabaseColumn(specialChars);
        String decrypted = encryptor.convertToEntityAttribute(encrypted);

        // Then
        assertEquals(specialChars, decrypted, "Special characters should decrypt correctly");
    }

    @Test
    @DisplayName("Should return null when encrypting null input")
    void testEncryptNull() {
        // When
        String encrypted = encryptor.convertToDatabaseColumn(null);

        // Then
        assertNull(encrypted, "Encrypting null should return null");
    }

    @Test
    @DisplayName("Should return null when encrypting empty string")
    void testEncryptEmptyString() {
        // When
        String encrypted = encryptor.convertToDatabaseColumn("");

        // Then
        assertNull(encrypted, "Encrypting empty string should return null");
    }

    @Test
    @DisplayName("Should return null when encrypting blank string")
    void testEncryptBlankString() {
        // When
        String encrypted = encryptor.convertToDatabaseColumn("   ");

        // Then
        assertNull(encrypted, "Encrypting blank string should return null");
    }

    @Test
    @DisplayName("Should return null when decrypting null input")
    void testDecryptNull() {
        // When
        String decrypted = encryptor.convertToEntityAttribute(null);

        // Then
        assertNull(decrypted, "Decrypting null should return null");
    }

    @Test
    @DisplayName("Should generate different ciphertexts for same plaintext (IV uniqueness)")
    void testIVUniqueness() {
        // Given
        String plainText = "John Doe";

        // When
        String encrypted1 = encryptor.convertToDatabaseColumn(plainText);
        String encrypted2 = encryptor.convertToDatabaseColumn(plainText);

        // Then
        assertNotEquals(encrypted1, encrypted2,
                "Same plaintext should produce different ciphertexts (IV randomness)");

        // But both should decrypt to same value
        assertEquals(plainText, encryptor.convertToEntityAttribute(encrypted1));
        assertEquals(plainText, encryptor.convertToEntityAttribute(encrypted2));
    }

    @Test
    @DisplayName("Should handle backward compatibility - plaintext data (lazy encryption)")
    void testBackwardCompatibility() {
        // Given - Simulate legacy plaintext data in database
        String plaintextLegacyData = "john.doe@example.com";

        // When - Attempt to decrypt (will fail, fallback to plaintext)
        String result = encryptor.convertToEntityAttribute(plaintextLegacyData);

        // Then - Should return plaintext as-is
        assertEquals(plaintextLegacyData, result,
                "Legacy plaintext data should be returned as-is");
    }

    @Test
    @DisplayName("Should handle invalid Base64 gracefully (backward compatibility)")
    void testInvalidBase64() {
        // Given - Invalid Base64 string
        String invalidBase64 = "not-valid-base64!!!";

        // When
        String result = encryptor.convertToEntityAttribute(invalidBase64);

        // Then - Should return plaintext as-is (backward compatibility)
        assertEquals(invalidBase64, result,
                "Invalid Base64 should be treated as plaintext");
    }

    @Test
    @DisplayName("Should warn and store plaintext when encryption key is missing")
    void testMissingEncryptionKey() {
        // Given - Encryptor without key
        PiiEncryptor encryptorWithoutKey = new PiiEncryptor();
        ReflectionTestUtils.setField(encryptorWithoutKey, "encryptionSecret", null);

        String plainText = "John Doe";

        // When
        String result = encryptorWithoutKey.convertToDatabaseColumn(plainText);

        // Then - Should store plaintext (with warning logged)
        assertEquals(plainText, result,
                "Without encryption key, should store plaintext");
    }

    @Test
    @DisplayName("Should throw exception on invalid key size")
    void testInvalidKeySize() {
        // Given - Invalid key (wrong length)
        PiiEncryptor invalidKeyEncryptor = new PiiEncryptor();
        ReflectionTestUtils.setField(invalidKeyEncryptor, "encryptionSecret", "short_key");

        String plainText = "John Doe";

        // When & Then
        assertThrows(
                PiiEncryptor.EncryptionException.class,
                () -> invalidKeyEncryptor.convertToDatabaseColumn(plainText),
                "Invalid key size should throw EncryptionException"
        );
    }

    @Test
    @DisplayName("Should decrypt successfully with same key")
    void testDecryptionWithSameKey() {
        // Given
        String plainText = "Sensitive Information";
        String encrypted = encryptor.convertToDatabaseColumn(plainText);

        // When - Create new encryptor instance with same key
        PiiEncryptor newEncryptor = new PiiEncryptor();
        ReflectionTestUtils.setField(newEncryptor, "encryptionSecret", TEST_KEY);
        String decrypted = newEncryptor.convertToEntityAttribute(encrypted);

        // Then
        assertEquals(plainText, decrypted,
                "Different encryptor instance with same key should decrypt successfully");
    }

    @Test
    @DisplayName("Should fail to decrypt with different key")
    void testDecryptionWithDifferentKey() {
        // Given
        String plainText = "Sensitive Information";
        String encrypted = encryptor.convertToDatabaseColumn(plainText);

        // When - Create new encryptor instance with different key
        PiiEncryptor differentKeyEncryptor = new PiiEncryptor();
        String differentKey = "abcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcd";
        ReflectionTestUtils.setField(differentKeyEncryptor, "encryptionSecret", differentKey);

        // Then - Decryption should fail gracefully and return encrypted string as plaintext
        String result = differentKeyEncryptor.convertToEntityAttribute(encrypted);
        assertEquals(encrypted, result,
                "Decryption with wrong key should return encrypted string as plaintext (backward compatibility)");
    }

    @Test
    @DisplayName("Should validate encrypted output is Base64")
    void testEncryptedOutputIsBase64() {
        // Given
        String plainText = "test@example.com";

        // When
        String encrypted = encryptor.convertToDatabaseColumn(plainText);

        // Then
        assertDoesNotThrow(
                () -> java.util.Base64.getDecoder().decode(encrypted),
                "Encrypted output should be valid Base64"
        );
    }

    @Test
    @DisplayName("Should handle whitespace-only strings")
    void testWhitespaceOnlyString() {
        // Given
        String whitespace = "   \t\n   ";

        // When
        String encrypted = encryptor.convertToDatabaseColumn(whitespace);

        // Then
        assertNull(encrypted, "Whitespace-only string should return null");
    }
}
