package com.affiliate.rentals.gydi.referrals.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

/**
 * Tests for ReferralLink.renew() domain method
 *
 * PHASE 2: Manual renewal of referral links
 */
class ReferralLinkRenewTest {

    @Test
    void shouldRenewLink_whenValidData() {
        // Given
        LocalDateTime originalExpiration = LocalDateTime.now().plusDays(5);
        ReferralLink link = ReferralLink.create(
                100L,
                200L,
                "old-token",
                "ABC12345",
                originalExpiration
        );

        String newToken = "new-encrypted-token";
        LocalDateTime newExpiration = LocalDateTime.now().plusDays(90);

        // When
        link.renew(newToken, newExpiration);

        // Then
        assertEquals(newToken, link.getEncryptedToken());
        assertEquals(newExpiration, link.getExpiresAt());
        assertEquals(ReferralLinkStatus.ACTIVE, link.getStatus());
        assertEquals("ABC12345", link.getShortCode()); // Same short code
        assertNotNull(link.getUpdatedAt());
    }

    @Test
    void shouldReactivateExpiredLink_whenRenewed() {
        // Given - Create link with future expiration first
        LocalDateTime futureExpiration = LocalDateTime.now().plusDays(1);
        ReferralLink link = ReferralLink.create(
                100L,
                200L,
                "old-token",
                "EXPIRED1",
                futureExpiration
        );
        // Then manually mark as expired
        link.markAsExpired();

        assertEquals(ReferralLinkStatus.EXPIRED, link.getStatus());

        // When
        String newToken = "renewed-token";
        LocalDateTime newExpiration = LocalDateTime.now().plusDays(30);
        link.renew(newToken, newExpiration);

        // Then
        assertEquals(ReferralLinkStatus.ACTIVE, link.getStatus());
        assertEquals(newToken, link.getEncryptedToken());
        assertEquals(newExpiration, link.getExpiresAt());
    }

    @Test
    void shouldReactivatePausedLink_whenRenewed() {
        // Given
        ReferralLink link = ReferralLink.create(
                100L,
                200L,
                "token",
                "PAUSED1",
                LocalDateTime.now().plusDays(10)
        );
        link.deactivate(); // Status becomes PAUSED

        assertEquals(ReferralLinkStatus.PAUSED, link.getStatus());

        // When
        String newToken = "new-token";
        LocalDateTime newExpiration = LocalDateTime.now().plusDays(60);
        link.renew(newToken, newExpiration);

        // Then
        assertEquals(ReferralLinkStatus.ACTIVE, link.getStatus());
    }

    @Test
    void shouldThrowException_whenTokenIsNull() {
        // Given
        ReferralLink link = ReferralLink.create(
                100L,
                200L,
                "token",
                "CODE123",
                LocalDateTime.now().plusDays(10)
        );

        LocalDateTime newExpiration = LocalDateTime.now().plusDays(30);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> link.renew(null, newExpiration)
        );

        assertTrue(exception.getMessage().contains("token cannot be empty"));
    }

    @Test
    void shouldThrowException_whenTokenIsBlank() {
        // Given
        ReferralLink link = ReferralLink.create(
                100L,
                200L,
                "token",
                "CODE123",
                LocalDateTime.now().plusDays(10)
        );

        LocalDateTime newExpiration = LocalDateTime.now().plusDays(30);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> link.renew("   ", newExpiration)
        );

        assertTrue(exception.getMessage().contains("token cannot be empty"));
    }

    @Test
    void shouldThrowException_whenExpirationIsNull() {
        // Given
        ReferralLink link = ReferralLink.create(
                100L,
                200L,
                "token",
                "CODE123",
                LocalDateTime.now().plusDays(10)
        );

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> link.renew("new-token", null)
        );

        assertTrue(exception.getMessage().contains("must be in the future"));
    }

    @Test
    void shouldThrowException_whenExpirationIsInPast() {
        // Given
        ReferralLink link = ReferralLink.create(
                100L,
                200L,
                "token",
                "CODE123",
                LocalDateTime.now().plusDays(10)
        );

        LocalDateTime pastDate = LocalDateTime.now().minusDays(5);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> link.renew("new-token", pastDate)
        );

        assertTrue(exception.getMessage().contains("must be in the future"));
    }

    @Test
    void shouldThrowException_whenLinkIsDeleted() {
        // Given
        ReferralLink link = ReferralLink.create(
                100L,
                200L,
                "token",
                "DELETED1",
                LocalDateTime.now().plusDays(10)
        );
        link.delete(); // Status becomes DISABLED

        assertEquals(ReferralLinkStatus.DISABLED, link.getStatus());

        // When & Then
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> link.renew("new-token", LocalDateTime.now().plusDays(30))
        );

        assertTrue(exception.getMessage().contains("Cannot renew a deleted link"));
    }

    @Test
    void shouldUpdateTimestamp_whenRenewed() throws InterruptedException {
        // Given
        ReferralLink link = ReferralLink.create(
                100L,
                200L,
                "token",
                "TIME123",
                LocalDateTime.now().plusDays(10)
        );

        LocalDateTime originalUpdatedAt = link.getUpdatedAt();

        // Wait a bit to ensure timestamp difference
        Thread.sleep(10);

        // When
        link.renew("new-token", LocalDateTime.now().plusDays(30));

        // Then
        assertTrue(link.getUpdatedAt().isAfter(originalUpdatedAt));
    }
}
