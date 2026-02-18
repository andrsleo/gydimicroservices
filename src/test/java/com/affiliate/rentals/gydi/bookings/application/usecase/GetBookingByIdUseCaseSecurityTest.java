package com.affiliate.rentals.gydi.bookings.application.usecase;

import com.affiliate.rentals.gydi.bookings.application.dto.BookingDto;
import com.affiliate.rentals.gydi.bookings.application.mapper.BookingMapper;
import com.affiliate.rentals.gydi.bookings.domain.exception.BookingAccessDeniedException;
import com.affiliate.rentals.gydi.bookings.domain.exception.BookingNotFoundException;
import com.affiliate.rentals.gydi.bookings.domain.model.Booking;
import com.affiliate.rentals.gydi.bookings.domain.ports.BookingRepositoryPort;
import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.model.PropertyId;
import com.affiliate.rentals.gydi.properties.domain.ports.out.PropertyRepositoryPort;
import com.affiliate.rentals.gydi.referrals.domain.model.ReferralLink;
import com.affiliate.rentals.gydi.referrals.domain.port.ReferralLinkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Security tests for GetBookingByIdUseCase - IDOR Prevention.
 * <p>
 * Tests verify that only the owner (affiliate or host) or ADMIN can access a booking.
 * </p>
 *
 * @author GYDI Security Team
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("GetBookingByIdUseCase - IDOR Prevention Tests")
class GetBookingByIdUseCaseSecurityTest {

    @Mock
    private BookingRepositoryPort bookingRepository;

    @Mock
    private BookingMapper bookingMapper;

    @Mock
    private ReferralLinkRepository referralLinkRepository;

    @Mock
    private PropertyRepositoryPort propertyRepository;

    private GetBookingByIdUseCase useCase;

    private static final Long BOOKING_ID = 1L;
    private static final Long AFFILIATE_USER_ID = 100L;
    private static final Long HOST_USER_ID = 200L;
    private static final Long UNAUTHORIZED_USER_ID = 999L;
    private static final Long REFERRAL_LINK_ID = 10L;
    private static final Long PROPERTY_ID = 50L;

    @Mock
    private Booking mockBooking;

    @Mock
    private BookingDto mockBookingDto;

    @BeforeEach
    void setUp() {
        useCase = new GetBookingByIdUseCase(
            bookingRepository,
            bookingMapper,
            referralLinkRepository,
            propertyRepository
        );

        // Setup booking mock to return IDs
        when(mockBooking.getReferralLinkId()).thenReturn(REFERRAL_LINK_ID);
        when(mockBooking.getPropertyId()).thenReturn(PROPERTY_ID);

        // Setup DTO mock
        when(mockBookingDto.id()).thenReturn(BOOKING_ID);
    }

    // ==================== POSITIVE TESTS ====================

    @Test
    @DisplayName("✅ ADMIN can access any booking")
    void adminCanAccessAnyBooking() {
        // Given: Booking exists
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(mockBooking));
        when(bookingMapper.toDto(mockBooking)).thenReturn(mockBookingDto);

        // When: ADMIN requests the booking (isAdmin = true)
        BookingDto result = useCase.execute(BOOKING_ID, UNAUTHORIZED_USER_ID, true);

        // Then: Access is granted
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(BOOKING_ID);
    }

    @Test
    @DisplayName("✅ Affiliate owner can access their booking")
    void affiliateOwnerCanAccessBooking() {
        // Given: Booking exists and user is the affiliate
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(mockBooking));
        when(bookingMapper.toDto(mockBooking)).thenReturn(mockBookingDto);

        ReferralLink mockReferralLink = mock(ReferralLink.class);
        when(mockReferralLink.getAffiliateId()).thenReturn(AFFILIATE_USER_ID);
        when(referralLinkRepository.findById(REFERRAL_LINK_ID))
            .thenReturn(Optional.of(mockReferralLink));

        // When: Affiliate requests their booking
        BookingDto result = useCase.execute(BOOKING_ID, AFFILIATE_USER_ID, false);

        // Then: Access is granted
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(BOOKING_ID);
    }

    @Test
    @DisplayName("✅ Host owner can access their booking")
    void hostOwnerCanAccessBooking() {
        // Given: Booking exists and user is the property host
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(mockBooking));
        when(bookingMapper.toDto(mockBooking)).thenReturn(mockBookingDto);

        // Affiliate is someone else
        ReferralLink mockReferralLink = mock(ReferralLink.class);
        when(mockReferralLink.getAffiliateId()).thenReturn(AFFILIATE_USER_ID);
        when(referralLinkRepository.findById(REFERRAL_LINK_ID))
            .thenReturn(Optional.of(mockReferralLink));

        // But user owns the property
        Property mockProperty = mock(Property.class);
        when(mockProperty.getHostId()).thenReturn(HOST_USER_ID);
        when(propertyRepository.findById(PropertyId.of(PROPERTY_ID)))
            .thenReturn(Optional.of(mockProperty));

        // When: Host requests booking for their property
        BookingDto result = useCase.execute(BOOKING_ID, HOST_USER_ID, false);

        // Then: Access is granted
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(BOOKING_ID);
    }

    // ==================== NEGATIVE TESTS (IDOR Prevention) ====================

    @Test
    @DisplayName("🛡️ IDOR BLOCKED: Unauthorized user cannot access booking")
    void unauthorizedUserCannotAccessBooking() {
        // Given: Booking exists but user is neither affiliate nor host
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(mockBooking));

        // User is NOT the affiliate
        ReferralLink mockReferralLink = mock(ReferralLink.class);
        when(mockReferralLink.getAffiliateId()).thenReturn(AFFILIATE_USER_ID);
        when(referralLinkRepository.findById(REFERRAL_LINK_ID))
            .thenReturn(Optional.of(mockReferralLink));

        // User is NOT the host
        Property mockProperty = mock(Property.class);
        when(mockProperty.getHostId()).thenReturn(HOST_USER_ID);
        when(propertyRepository.findById(PropertyId.of(PROPERTY_ID)))
            .thenReturn(Optional.of(mockProperty));

        // When & Then: Unauthorized user tries to access booking
        assertThatThrownBy(() -> useCase.execute(BOOKING_ID, UNAUTHORIZED_USER_ID, false))
            .isInstanceOf(BookingAccessDeniedException.class)
            .hasMessageContaining("You do not have permission to view this booking");
    }

    @Test
    @DisplayName("🛡️ IDOR BLOCKED: Affiliate A cannot access Affiliate B's booking")
    void affiliateCannotAccessOtherAffiliateBooking() {
        // Given: Booking exists for Affiliate A
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(mockBooking));

        ReferralLink mockReferralLink = mock(ReferralLink.class);
        when(mockReferralLink.getAffiliateId()).thenReturn(AFFILIATE_USER_ID); // Affiliate A
        when(referralLinkRepository.findById(REFERRAL_LINK_ID))
            .thenReturn(Optional.of(mockReferralLink));

        // No property ownership
        when(propertyRepository.findById(PropertyId.of(PROPERTY_ID)))
            .thenReturn(Optional.empty());

        // When & Then: Affiliate B (ID=888) tries to access Affiliate A's booking
        Long otherAffiliateId = 888L;
        assertThatThrownBy(() -> useCase.execute(BOOKING_ID, otherAffiliateId, false))
            .isInstanceOf(BookingAccessDeniedException.class)
            .hasMessageContaining("You do not have permission to view this booking");
    }

    @Test
    @DisplayName("🛡️ IDOR BLOCKED: Host A cannot access Host B's booking")
    void hostCannotAccessOtherHostBooking() {
        // Given: Booking exists for Host A's property
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(mockBooking));

        // No affiliate ownership
        when(referralLinkRepository.findById(REFERRAL_LINK_ID))
            .thenReturn(Optional.empty());

        // Property belongs to Host A
        Property mockProperty = mock(Property.class);
        when(mockProperty.getHostId()).thenReturn(HOST_USER_ID); // Host A
        when(propertyRepository.findById(PropertyId.of(PROPERTY_ID)))
            .thenReturn(Optional.of(mockProperty));

        // When & Then: Host B (ID=777) tries to access Host A's booking
        Long otherHostId = 777L;
        assertThatThrownBy(() -> useCase.execute(BOOKING_ID, otherHostId, false))
            .isInstanceOf(BookingAccessDeniedException.class)
            .hasMessageContaining("You do not have permission to view this booking");
    }

    @Test
    @DisplayName("🛡️ Booking not found throws BookingNotFoundException")
    void bookingNotFoundThrowsException() {
        // Given: Booking does not exist
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.empty());

        // When & Then: Should throw BookingNotFoundException (even for ADMIN)
        assertThatThrownBy(() -> useCase.execute(BOOKING_ID, UNAUTHORIZED_USER_ID, true))
            .isInstanceOf(BookingNotFoundException.class);
    }

    // ==================== EDGE CASES ====================

    @Test
    @DisplayName("🔍 Edge Case: Referral link not found - only host can access")
    void whenReferralLinkNotFound_OnlyHostCanAccess() {
        // Given: Booking exists but referral link is deleted
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(mockBooking));
        when(bookingMapper.toDto(mockBooking)).thenReturn(mockBookingDto);

        // Referral link not found
        when(referralLinkRepository.findById(REFERRAL_LINK_ID))
            .thenReturn(Optional.empty());

        // User owns the property
        Property mockProperty = mock(Property.class);
        when(mockProperty.getHostId()).thenReturn(HOST_USER_ID);
        when(propertyRepository.findById(PropertyId.of(PROPERTY_ID)))
            .thenReturn(Optional.of(mockProperty));

        // When: Host accesses booking
        BookingDto result = useCase.execute(BOOKING_ID, HOST_USER_ID, false);

        // Then: Access granted to host
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("🔍 Edge Case: Property not found - only affiliate can access")
    void whenPropertyNotFound_OnlyAffiliateCanAccess() {
        // Given: Booking exists but property is deleted
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(mockBooking));
        when(bookingMapper.toDto(mockBooking)).thenReturn(mockBookingDto);

        // User owns the referral link
        ReferralLink mockReferralLink = mock(ReferralLink.class);
        when(mockReferralLink.getAffiliateId()).thenReturn(AFFILIATE_USER_ID);
        when(referralLinkRepository.findById(REFERRAL_LINK_ID))
            .thenReturn(Optional.of(mockReferralLink));

        // Property not found
        when(propertyRepository.findById(PropertyId.of(PROPERTY_ID)))
            .thenReturn(Optional.empty());

        // When: Affiliate accesses booking
        BookingDto result = useCase.execute(BOOKING_ID, AFFILIATE_USER_ID, false);

        // Then: Access granted to affiliate
        assertThat(result).isNotNull();
    }
}
