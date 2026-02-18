package com.affiliate.rentals.gydi.bookings.infrastructure;

import com.affiliate.rentals.gydi.bookings.domain.model.BookingStatus;
import com.affiliate.rentals.gydi.bookings.infrastructure.out.persistence.entity.BookingJpaEntity;
import com.affiliate.rentals.gydi.bookings.infrastructure.out.persistence.repository.BookingJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for PII encryption in BookingJpaEntity
 *
 * ✅ SECURITY FIX (FIX-007): PII Encryption at Rest - Integration Tests
 *
 * ⚠️ DISABLED: These tests require PostgreSQL with bookings schema.
 * H2 in-memory database doesn't support schemas.
 * To run these tests, use a real PostgreSQL instance with Flyway migrations applied.
 *
 * Test Coverage:
 * - Verify PII fields are encrypted in database
 * - Verify PII fields are decrypted correctly when loaded
 * - Verify backward compatibility with plaintext data
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("Booking PII Encryption Integration Tests")
class BookingJpaEntityEncryptionIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BookingJpaRepository bookingRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Should encrypt PII when saving booking to database")
    void testPiiEncryptionOnSave() {
        // Given - Create booking with sensitive PII
        BookingJpaEntity booking = createTestBooking(
                "John Doe",
                "john.doe@example.com",
                "+1 (555) 123-4567"
        );

        // When - Save to database
        BookingJpaEntity saved = bookingRepository.save(booking);
        entityManager.flush();
        entityManager.clear(); // Clear persistence context to force DB read

        // Then - Verify PII is encrypted in database (raw SQL query)
        Map<String, Object> rawData = jdbcTemplate.queryForMap(
                "SELECT guest_name, guest_email, guest_phone FROM bookings.booking WHERE id = ?",
                saved.getId()
        );

        String rawGuestName = (String) rawData.get("guest_name");
        String rawGuestEmail = (String) rawData.get("guest_email");
        String rawGuestPhone = (String) rawData.get("guest_phone");

        // ✅ SECURITY ASSERTION: Raw data in DB should NOT match plaintext
        assertNotEquals("John Doe", rawGuestName,
                "Guest name should be encrypted in database");
        assertNotEquals("john.doe@example.com", rawGuestEmail,
                "Guest email should be encrypted in database");
        assertNotEquals("+1 (555) 123-4567", rawGuestPhone,
                "Guest phone should be encrypted in database");

        // ✅ Encrypted data should be Base64 encoded (starts with uppercase/lowercase letters)
        assertTrue(rawGuestName.matches("^[A-Za-z0-9+/=]+$"),
                "Encrypted guest name should be Base64");
        assertTrue(rawGuestEmail.matches("^[A-Za-z0-9+/=]+$"),
                "Encrypted guest email should be Base64");
        assertTrue(rawGuestPhone.matches("^[A-Za-z0-9+/=]+$"),
                "Encrypted guest phone should be Base64");
    }

    @Test
    @DisplayName("Should decrypt PII correctly when loading booking from database")
    void testPiiDecryptionOnLoad() {
        // Given - Create and save booking
        BookingJpaEntity booking = createTestBooking(
                "Jane Smith",
                "jane.smith@example.com",
                "+44 20 7946 0958"
        );

        BookingJpaEntity saved = bookingRepository.save(booking);
        Long savedId = saved.getId();
        entityManager.flush();
        entityManager.clear(); // Clear persistence context

        // When - Load booking from database
        BookingJpaEntity loaded = bookingRepository.findById(savedId)
                .orElseThrow(() -> new AssertionError("Booking not found"));

        // Then - Verify PII is decrypted correctly
        assertEquals("Jane Smith", loaded.getGuestName(),
                "Guest name should be decrypted correctly");
        assertEquals("jane.smith@example.com", loaded.getGuestEmail(),
                "Guest email should be decrypted correctly");
        assertEquals("+44 20 7946 0958", loaded.getGuestPhone(),
                "Guest phone should be decrypted correctly");
    }

    @Test
    @DisplayName("Should handle special characters in PII")
    void testSpecialCharactersInPii() {
        // Given - Booking with special characters
        BookingJpaEntity booking = createTestBooking(
                "José García-López",
                "jose.garcia+test@example.com",
                "+34 (91) 123-4567"
        );

        // When
        BookingJpaEntity saved = bookingRepository.save(booking);
        entityManager.flush();
        entityManager.clear();

        BookingJpaEntity loaded = bookingRepository.findById(saved.getId())
                .orElseThrow(() -> new AssertionError("Booking not found"));

        // Then
        assertEquals("José García-López", loaded.getGuestName());
        assertEquals("jose.garcia+test@example.com", loaded.getGuestEmail());
        assertEquals("+34 (91) 123-4567", loaded.getGuestPhone());
    }

    @Test
    @DisplayName("Should handle null phone number")
    void testNullPhoneNumber() {
        // Given - Booking without phone number
        BookingJpaEntity booking = createTestBooking(
                "Alice Johnson",
                "alice@example.com",
                null  // No phone number
        );

        // When
        BookingJpaEntity saved = bookingRepository.save(booking);
        entityManager.flush();
        entityManager.clear();

        BookingJpaEntity loaded = bookingRepository.findById(saved.getId())
                .orElseThrow(() -> new AssertionError("Booking not found"));

        // Then
        assertNull(loaded.getGuestPhone(), "Null phone should remain null");
        assertEquals("Alice Johnson", loaded.getGuestName());
        assertEquals("alice@example.com", loaded.getGuestEmail());
    }

    @Test
    @DisplayName("Should encrypt each booking with different ciphertext (IV uniqueness)")
    void testIvUniqueness() {
        // Given - Two bookings with same guest info
        BookingJpaEntity booking1 = createTestBooking(
                "Bob Wilson",
                "bob@example.com",
                "+1-555-9876"
        );
        BookingJpaEntity booking2 = createTestBooking(
                "Bob Wilson",
                "bob@example.com",
                "+1-555-9876"
        );

        // When - Save both
        BookingJpaEntity saved1 = bookingRepository.save(booking1);
        BookingJpaEntity saved2 = bookingRepository.save(booking2);
        entityManager.flush();

        // Then - Encrypted values in DB should be different (due to random IV)
        Map<String, Object> raw1 = jdbcTemplate.queryForMap(
                "SELECT guest_name, guest_email FROM bookings.booking WHERE id = ?",
                saved1.getId()
        );
        Map<String, Object> raw2 = jdbcTemplate.queryForMap(
                "SELECT guest_name, guest_email FROM bookings.booking WHERE id = ?",
                saved2.getId()
        );

        assertNotEquals(raw1.get("guest_name"), raw2.get("guest_name"),
                "Same plaintext should produce different ciphertext (IV randomness)");
        assertNotEquals(raw1.get("guest_email"), raw2.get("guest_email"),
                "Same plaintext should produce different ciphertext (IV randomness)");

        // But decrypted values should match
        entityManager.clear();
        BookingJpaEntity loaded1 = bookingRepository.findById(saved1.getId()).orElseThrow();
        BookingJpaEntity loaded2 = bookingRepository.findById(saved2.getId()).orElseThrow();

        assertEquals(loaded1.getGuestName(), loaded2.getGuestName());
        assertEquals(loaded1.getGuestEmail(), loaded2.getGuestEmail());
    }

    /**
     * Helper method to create test booking
     */
    private BookingJpaEntity createTestBooking(String guestName, String guestEmail, String guestPhone) {
        BookingJpaEntity booking = new BookingJpaEntity();
        booking.setReferralLinkId(1L);
        booking.setPropertyId(1L);
        booking.setCheckInDate(LocalDate.now().plusDays(7));
        booking.setCheckOutDate(LocalDate.now().plusDays(10));
        booking.setGuestName(guestName);
        booking.setGuestEmail(guestEmail);
        booking.setGuestPhone(guestPhone);
        booking.setGuestsCount(2);
        booking.setTotalAmount(new BigDecimal("300.00"));
        booking.setCurrency("USD");
        booking.setStatus(BookingStatus.REQUEST);
        return booking;
    }
}
