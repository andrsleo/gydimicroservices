package com.affiliate.rentals.gydi.referrals;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.affiliate.rentals.gydi.shared.security.JwtReferralTokenService;
import com.affiliate.rentals.gydi.shared.security.JwtReferralTokenService.InvalidReferralTokenException;
import com.affiliate.rentals.gydi.shared.security.JwtReferralTokenService.ReferralTokenData;

class ReferralTokenEncryptionTest {

    private JwtReferralTokenService tokenService;
    private static final String TEST_SECRET = "12345678901234567890123456789012"; // 32 bytes
    private static final long TEST_EXPIRATION = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        tokenService = new JwtReferralTokenService(TEST_SECRET, TEST_EXPIRATION);
    }

    @Test
    void shouldEncryptAndDecryptTokenSuccessfully() {
        Long userId = 100L;
        Long propertyId = 500L;

        String token = tokenService.generateReferralToken(userId, propertyId);

        assertThat(token).isNotNull().isNotEmpty();
        // JWE tokens have 5 parts (header.encryptedKey.iv.ciphertext.tag)
        assertThat(token.split("\\.")).hasSize(5);

        ReferralTokenData data = tokenService.validateAndExtract(token);
        assertThat(data.userId()).isEqualTo(userId);
        assertThat(data.propertyId()).isEqualTo(propertyId);
    }

    @Test
    void shouldFailWithInvalidSecret() {
        // Create a service with a different secret
        JwtReferralTokenService otherService = new JwtReferralTokenService(
                "different_secret_key_32_bytes_long_!!", TEST_EXPIRATION);

        String token = tokenService.generateReferralToken(1L, 1L);

        // Try to decrypt with the wrong key
        assertThrows(InvalidReferralTokenException.class, () -> {
            otherService.validateAndExtract(token);
        });
    }

    @Test
    void shouldFailWithTamperedToken() {
        String token = tokenService.generateReferralToken(1L, 1L);

        // Tamper with the token (change a character in the middle)
        int middleIndex = token.length() / 2;
        char middleChar = token.charAt(middleIndex);
        char newChar = (middleChar == 'X') ? 'Y' : 'X';
        // Ensure we don't replace a dot separator
        if (middleChar == '.') {
            middleIndex++;
            newChar = 'Z';
        }
        String tamperedToken = token.substring(0, middleIndex) + newChar + token.substring(middleIndex + 1);

        try {
            tokenService.validateAndExtract(tamperedToken);
            org.junit.jupiter.api.Assertions.fail("Should have thrown InvalidReferralTokenException");
        } catch (InvalidReferralTokenException e) {
            // Expected
            assertThat(e.getMessage()).contains("Invalid or tampered referral token");
        }
    }

    @Test
    void shouldRejectExpiredToken() throws InterruptedException {
        // Service with 1ms expiration
        JwtReferralTokenService shortLivedService = new JwtReferralTokenService(TEST_SECRET, 1);

        String token = shortLivedService.generateReferralToken(1L, 1L);

        Thread.sleep(10); // Wait for expiration

        assertThrows(InvalidReferralTokenException.class, () -> {
            shortLivedService.validateAndExtract(token);
        });
    }
}
