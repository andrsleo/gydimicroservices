package com.affiliate.rentals.gydi.commissions.domain.model;

import com.affiliate.rentals.gydi.commissions.domain.exception.InvalidCommissionStateException;
import com.affiliate.rentals.gydi.commissions.domain.model.vo.CommissionAmount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for HostCommission aggregate.
 */
class HostCommissionTest {

    @Test
    @DisplayName("Should create host commission in PENDING status")
    void shouldCreateHostCommissionInPendingStatus() {
        // Given
        Long bookingId = 123L;
        Long hostId = 456L;
        String hostPlan = "PRO";
        CommissionAmount amount = CommissionAmount.calculate(
            new BigDecimal("1000.00"),
            new BigDecimal("0.20"),
            "USD"
        );

        // When
        HostCommission commission = HostCommission.create(bookingId, hostId, hostPlan, amount);

        // Then
        assertNotNull(commission);
        assertEquals(bookingId, commission.getBookingId());
        assertEquals(hostId, commission.getHostId());
        assertEquals(hostPlan, commission.getHostPlan());
        assertEquals(HostCommissionStatus.PENDING, commission.getStatus());
        assertEquals(new BigDecimal("200.00"), commission.getAmount().getCommissionAmount());
        assertEquals(0, commission.getAttemptCount());
        assertNull(commission.getChargedAt());
    }

    @Test
    @DisplayName("Should transition from PENDING to PROCESSING")
    void shouldTransitionToPendingToProcessing() {
        // Given
        HostCommission commission = createTestCommission();
        String paymentIntentId = "pi_test_123";

        // When
        commission.markAsProcessing(paymentIntentId);

        // Then
        assertEquals(HostCommissionStatus.PROCESSING, commission.getStatus());
        assertEquals(paymentIntentId, commission.getStripePaymentIntentId());
        assertEquals(1, commission.getAttemptCount());
        assertNotNull(commission.getLastAttemptAt());
    }

    @Test
    @DisplayName("Should transition from PROCESSING to CHARGED")
    void shouldTransitionToCharged() {
        // Given
        HostCommission commission = createTestCommission();
        commission.markAsProcessing("pi_test_123");
        String chargeId = "ch_test_456";

        // When
        commission.markAsCharged(chargeId);

        // Then
        assertEquals(HostCommissionStatus.CHARGED, commission.getStatus());
        assertEquals(chargeId, commission.getStripeChargeId());
        assertNotNull(commission.getChargedAt());
        assertNull(commission.getFailureReason());
        assertTrue(commission.isTerminal());
    }

    @Test
    @DisplayName("Should mark as FAILED and schedule retry")
    void shouldMarkAsFailedAndScheduleRetry() {
        // Given
        HostCommission commission = createTestCommission();
        commission.markAsProcessing("pi_test_123");
        String failureReason = "Insufficient funds";

        // When
        commission.markAsFailed(failureReason);

        // Then
        assertEquals(HostCommissionStatus.FAILED, commission.getStatus());
        assertEquals(failureReason, commission.getFailureReason());
        assertNotNull(commission.getNextRetryAt());
        assertTrue(commission.canRetry());
    }

    @Test
    @DisplayName("Should not allow retry after max attempts")
    void shouldNotAllowRetryAfterMaxAttempts() {
        // Given
        HostCommission commission = createTestCommission();

        // When: Simulate 3 failed attempts
        for (int i = 0; i < 3; i++) {
            commission.markAsProcessing("pi_test_" + i);
            commission.markAsFailed("Test failure");
        }

        // Then
        assertFalse(commission.canRetry());
        assertNull(commission.getNextRetryAt());
    }

    @Test
    @DisplayName("Should throw exception when marking charged from invalid status")
    void shouldThrowExceptionWhenMarkingChargedFromInvalidStatus() {
        // Given
        HostCommission commission = createTestCommission();

        // When & Then
        assertThrows(InvalidCommissionStateException.class, () -> {
            commission.markAsCharged("ch_test");
        });
    }

    @Test
    @DisplayName("Should refund a charged commission")
    void shouldRefundChargedCommission() {
        // Given
        HostCommission commission = createTestCommission();
        commission.markAsProcessing("pi_test");
        commission.markAsCharged("ch_test");
        String refundReason = "Booking disputed by guest";

        // When
        commission.refund(refundReason);

        // Then
        assertEquals(HostCommissionStatus.REFUNDED, commission.getStatus());
        assertEquals(refundReason, commission.getFailureReason());
        assertTrue(commission.isTerminal());
    }

    @Test
    @DisplayName("Should not allow refund of non-charged commission")
    void shouldNotAllowRefundOfNonChargedCommission() {
        // Given
        HostCommission commission = createTestCommission();

        // When & Then
        assertThrows(InvalidCommissionStateException.class, () -> {
            commission.refund("Test refund");
        });
    }

    // Helper method
    private HostCommission createTestCommission() {
        CommissionAmount amount = CommissionAmount.calculate(
            new BigDecimal("1000.00"),
            new BigDecimal("0.20"),
            "USD"
        );
        return HostCommission.create(123L, 456L, "PRO", amount);
    }
}
