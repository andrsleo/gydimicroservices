package com.affiliate.rentals.gydi.referrals.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.affiliate.rentals.gydi.referrals.application.dto.RenewReferralLinkResponse;
import com.affiliate.rentals.gydi.referrals.domain.model.ReferralLink;
import com.affiliate.rentals.gydi.referrals.domain.port.ReferralLinkRepository;
import com.affiliate.rentals.gydi.shared.security.JwtReferralTokenService;
import com.affiliate.rentals.gydi.users.domain.model.Email;
import com.affiliate.rentals.gydi.users.domain.model.Role;
import com.affiliate.rentals.gydi.users.domain.model.SubscriptionPlan;
import com.affiliate.rentals.gydi.users.domain.model.User;
import com.affiliate.rentals.gydi.users.domain.ports.UserRepositoryPort;

/**
 * Tests for RenewReferralLinkUseCase
 *
 * PHASE 2: Manual renewal of referral links
 */
@ExtendWith(MockitoExtension.class)
class RenewReferralLinkUseCaseTest {

    @Mock
    private ReferralLinkRepository referralLinkRepository;

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private JwtReferralTokenService jwtReferralTokenService;

    @InjectMocks
    private RenewReferralLinkUseCase renewReferralLinkUseCase;

    private static final String FRONTEND_URL = "https://test.com";

    @BeforeEach
    void setUp() {
        // Inject frontend URL via reflection since it's a constructor parameter
        renewReferralLinkUseCase = new RenewReferralLinkUseCase(
                referralLinkRepository,
                userRepository,
                jwtReferralTokenService,
                FRONTEND_URL
        );
    }

    @Test
    void shouldRenewLink_whenValidRequest_withFreePlan() {
        // Given
        Long linkId = 1L;
        Long userId = 100L;
        Long propertyId = 200L;

        ReferralLink existingLink = ReferralLink.create(
                userId,
                propertyId,
                "old-token",
                "ABC12345",
                LocalDateTime.now().plusDays(5) // Expires soon
        );
        existingLink.setId(linkId);

        User user = User.builder()
                .id(userId)
                .name("Test User")
                .email(Email.of("test@example.com"))
                .passwordHash("hash")
                .activePlan(SubscriptionPlan.FREE)
                .addRole(Role.user())
                .build();

        String newToken = "new-encrypted-token";

        when(referralLinkRepository.findById(linkId)).thenReturn(Optional.of(existingLink));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(jwtReferralTokenService.generateReferralToken(eq(userId), eq(propertyId), anyLong()))
                .thenReturn(newToken);
        when(referralLinkRepository.update(any(ReferralLink.class))).thenAnswer(i -> i.getArgument(0));

        // When
        RenewReferralLinkResponse response = renewReferralLinkUseCase.execute(linkId, userId);

        // Then
        assertNotNull(response);
        assertEquals(linkId, response.id());
        assertEquals(newToken, response.encryptedToken());
        assertEquals("ABC12345", response.shortCode()); // Same short code
        assertTrue(response.fullUrl().contains(newToken));

        // Verify user's plan was checked (FREE = 30 days)
        verify(jwtReferralTokenService).generateReferralToken(
                eq(userId),
                eq(propertyId),
                eq(30L * 24 * 60 * 60 * 1000) // 30 days in milliseconds
        );
        verify(referralLinkRepository).update(any(ReferralLink.class));
    }

    @Test
    void shouldRenewLink_withProPlan() {
        // Given
        Long linkId = 1L;
        Long userId = 100L;
        Long propertyId = 200L;

        ReferralLink existingLink = ReferralLink.create(
                userId,
                propertyId,
                "old-token",
                "XYZ98765",
                LocalDateTime.now().plusDays(1) // Created with future expiration
        );
        existingLink.setId(linkId);
        existingLink.markAsExpired(); // Then mark as expired

        User user = User.builder()
                .id(userId)
                .name("Pro User")
                .email(Email.of("pro@example.com"))
                .passwordHash("hash")
                .activePlan(SubscriptionPlan.PRO)
                .addRole(Role.user())
                .build();

        String newToken = "renewed-token";

        when(referralLinkRepository.findById(linkId)).thenReturn(Optional.of(existingLink));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(jwtReferralTokenService.generateReferralToken(eq(userId), eq(propertyId), anyLong()))
                .thenReturn(newToken);
        when(referralLinkRepository.update(any(ReferralLink.class))).thenAnswer(i -> i.getArgument(0));

        // When
        RenewReferralLinkResponse response = renewReferralLinkUseCase.execute(linkId, userId);

        // Then
        assertNotNull(response);

        // Verify PRO plan = 90 days
        verify(jwtReferralTokenService).generateReferralToken(
                eq(userId),
                eq(propertyId),
                eq(90L * 24 * 60 * 60 * 1000) // 90 days in milliseconds
        );
    }

    @Test
    void shouldRenewLink_withElitePlan() {
        // Given
        Long linkId = 1L;
        Long userId = 100L;
        Long propertyId = 200L;

        ReferralLink existingLink = ReferralLink.create(
                userId,
                propertyId,
                "old-token",
                "ELITE123",
                LocalDateTime.now().plusDays(10)
        );
        existingLink.setId(linkId);

        User user = User.builder()
                .id(userId)
                .name("Elite User")
                .email(Email.of("elite@example.com"))
                .passwordHash("hash")
                .activePlan(SubscriptionPlan.ELITE)
                .addRole(Role.user())
                .build();

        String newToken = "elite-token";

        when(referralLinkRepository.findById(linkId)).thenReturn(Optional.of(existingLink));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(jwtReferralTokenService.generateReferralToken(eq(userId), eq(propertyId), anyLong()))
                .thenReturn(newToken);
        when(referralLinkRepository.update(any(ReferralLink.class))).thenAnswer(i -> i.getArgument(0));

        // When
        RenewReferralLinkResponse response = renewReferralLinkUseCase.execute(linkId, userId);

        // Then
        assertNotNull(response);

        // Verify ELITE plan = 365 days
        verify(jwtReferralTokenService).generateReferralToken(
                eq(userId),
                eq(propertyId),
                eq(365L * 24 * 60 * 60 * 1000) // 365 days in milliseconds
        );
    }

    @Test
    void shouldThrowException_whenLinkNotFound() {
        // Given
        Long linkId = 999L;
        Long userId = 100L;

        when(referralLinkRepository.findById(linkId)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> renewReferralLinkUseCase.execute(linkId, userId)
        );

        assertTrue(exception.getMessage().contains("not found"));
        verify(referralLinkRepository, never()).update(any());
    }

    @Test
    void shouldThrowException_whenUserIsNotOwner() {
        // Given
        Long linkId = 1L;
        Long ownerId = 100L;
        Long attackerId = 999L; // Different user trying to renew

        ReferralLink existingLink = ReferralLink.create(
                ownerId,
                200L,
                "token",
                "CODE123",
                LocalDateTime.now().plusDays(10)
        );
        existingLink.setId(linkId);

        when(referralLinkRepository.findById(linkId)).thenReturn(Optional.of(existingLink));

        // When & Then
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> renewReferralLinkUseCase.execute(linkId, attackerId)
        );

        assertTrue(exception.getMessage().contains("Unauthorized"));
        verify(userRepository, never()).findById(any());
        verify(referralLinkRepository, never()).update(any());
    }

    @Test
    void shouldThrowException_whenUserNotFound() {
        // Given
        Long linkId = 1L;
        Long userId = 100L;

        ReferralLink existingLink = ReferralLink.create(
                userId,
                200L,
                "token",
                "CODE123",
                LocalDateTime.now().plusDays(10)
        );
        existingLink.setId(linkId);

        when(referralLinkRepository.findById(linkId)).thenReturn(Optional.of(existingLink));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> renewReferralLinkUseCase.execute(linkId, userId)
        );

        assertTrue(exception.getMessage().contains("User not found"));
        verify(referralLinkRepository, never()).update(any());
    }
}
