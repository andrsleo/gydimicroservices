package com.affiliate.rentals.gydi.commissions.domain.model;

import com.affiliate.rentals.gydi.commissions.domain.exception.InvalidCommissionStateException;
import com.affiliate.rentals.gydi.commissions.domain.model.vo.CommissionAmount;
import com.affiliate.rentals.gydi.commissions.domain.model.vo.DisputePeriod;
import com.affiliate.rentals.gydi.commissions.domain.model.vo.PaymentSchedule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ReferralCommission behavior related to the WAITING_HOST_CHARGE business rule.
 * <p>
 * Business Rule: Referral commissions can only be paid AFTER the host commission
 * is successfully charged. The initial status is WAITING_HOST_CHARGE.
 * </p>
 */
class ReferralCommissionWaitingHostChargeTest {

    private static final Long BOOKING_ID = 123L;
    private static final Long AFFILIATE_ID = 456L;
    private static final String AFFILIATE_PLAN = "PRO";

    private CommissionAmount amount;
    private LocalDateTime bookingFinishedAt;

    @BeforeEach
    void setUp() {
        amount = CommissionAmount.calculate(
            new BigDecimal("1000.00"),
            new BigDecimal("0.05"),
            "USD"
        );
        bookingFinishedAt = LocalDateTime.now().minusDays(1);
    }

    // ========== INITIAL STATUS TESTS ==========

    @Test
    @DisplayName("create() should initialize referral commission in WAITING_HOST_CHARGE status")
    void shouldCreateReferralCommissionInWaitingHostChargeStatus() {
        ReferralCommission commission = ReferralCommission.create(
            BOOKING_ID, AFFILIATE_ID, AFFILIATE_PLAN, amount, bookingFinishedAt
        );

        assertEquals(ReferralCommissionStatus.WAITING_HOST_CHARGE, commission.getStatus(),
            "New referral commission must start in WAITING_HOST_CHARGE, not PENDING");
        assertNull(commission.getFailureReason());
        assertEquals(0, commission.getAttemptCount());
    }

    // ========== approveAfterHostCharged() TESTS ==========

    @Test
    @DisplayName("approveAfterHostCharged() should transition WAITING_HOST_CHARGE to APPROVED")
    void shouldApproveAfterHostCharged() {
        ReferralCommission commission = ReferralCommission.create(
            BOOKING_ID, AFFILIATE_ID, AFFILIATE_PLAN, amount, bookingFinishedAt
        );
        assertEquals(ReferralCommissionStatus.WAITING_HOST_CHARGE, commission.getStatus());

        commission.approveAfterHostCharged();

        assertEquals(ReferralCommissionStatus.APPROVED, commission.getStatus(),
            "Commission should be APPROVED after host is charged");
        assertNotNull(commission.getUpdatedAt());
    }

    @Test
    @DisplayName("approveAfterHostCharged() should throw when commission is PENDING")
    void shouldThrowWhenApprovingFromPending() {
        ReferralCommission commission = buildCommissionInStatus(ReferralCommissionStatus.PENDING);

        assertThrows(InvalidCommissionStateException.class, commission::approveAfterHostCharged,
            "Cannot approve from PENDING via host charge path");
    }

    @Test
    @DisplayName("approveAfterHostCharged() should throw when commission is already APPROVED")
    void shouldThrowWhenApprovingFromApproved() {
        ReferralCommission commission = buildCommissionInStatus(ReferralCommissionStatus.APPROVED);

        assertThrows(InvalidCommissionStateException.class, commission::approveAfterHostCharged,
            "Cannot approve from APPROVED via host charge path");
    }

    @Test
    @DisplayName("approveAfterHostCharged() should throw when commission is PAID")
    void shouldThrowWhenApprovingFromPaid() {
        ReferralCommission commission = buildCommissionInStatus(ReferralCommissionStatus.PAID);

        assertThrows(InvalidCommissionStateException.class, commission::approveAfterHostCharged,
            "Cannot approve from PAID via host charge path");
    }

    @Test
    @DisplayName("approveAfterHostCharged() should throw when commission is CANCELLED")
    void shouldThrowWhenApprovingFromCancelled() {
        ReferralCommission commission = buildCommissionInStatus(ReferralCommissionStatus.CANCELLED);

        assertThrows(InvalidCommissionStateException.class, commission::approveAfterHostCharged,
            "Cannot approve from CANCELLED via host charge path");
    }

    @Test
    @DisplayName("approveAfterHostCharged() should throw when commission is WITHHELD")
    void shouldThrowWhenApprovingFromWithheld() {
        ReferralCommission commission = buildCommissionInStatus(ReferralCommissionStatus.WITHHELD);

        assertThrows(InvalidCommissionStateException.class, commission::approveAfterHostCharged,
            "Cannot approve from WITHHELD via host charge path");
    }

    // ========== withholdDueToHostChargeFailure() TESTS ==========

    @Test
    @DisplayName("withholdDueToHostChargeFailure() should transition WAITING_HOST_CHARGE to WITHHELD")
    void shouldWithholdDueToHostChargeFailure() {
        ReferralCommission commission = ReferralCommission.create(
            BOOKING_ID, AFFILIATE_ID, AFFILIATE_PLAN, amount, bookingFinishedAt
        );
        String reason = "Host commission for booking 123 not collected after 3 attempts";

        commission.withholdDueToHostChargeFailure(reason);

        assertEquals(ReferralCommissionStatus.WITHHELD, commission.getStatus(),
            "Commission should be WITHHELD when host charge fails definitively");
        assertEquals(reason, commission.getFailureReason());
        assertNotNull(commission.getUpdatedAt());
    }

    @Test
    @DisplayName("withholdDueToHostChargeFailure() should throw when commission is PENDING")
    void shouldThrowWhenWithholdingFromPending() {
        ReferralCommission commission = buildCommissionInStatus(ReferralCommissionStatus.PENDING);

        assertThrows(InvalidCommissionStateException.class,
            () -> commission.withholdDueToHostChargeFailure("reason"),
            "Cannot withhold due to host charge failure from PENDING");
    }

    @Test
    @DisplayName("withholdDueToHostChargeFailure() should throw when commission is APPROVED")
    void shouldThrowWhenWithholdingFromApproved() {
        ReferralCommission commission = buildCommissionInStatus(ReferralCommissionStatus.APPROVED);

        assertThrows(InvalidCommissionStateException.class,
            () -> commission.withholdDueToHostChargeFailure("reason"),
            "Cannot withhold due to host charge failure from APPROVED");
    }

    // ========== STATE MACHINE TESTS ==========

    @Test
    @DisplayName("canApprove() should return false for WAITING_HOST_CHARGE (uses approveAfterHostCharged path)")
    void canApproveShouldReturnFalseForWaitingHostCharge() {
        assertFalse(ReferralCommissionStatus.WAITING_HOST_CHARGE.canApprove(),
            "WAITING_HOST_CHARGE should not use the dispute-period approval path");
    }

    @Test
    @DisplayName("canApproveFromHostCharge() should return true only for WAITING_HOST_CHARGE")
    void canApproveFromHostChargeShouldReturnTrueOnlyForWaitingHostCharge() {
        assertTrue(ReferralCommissionStatus.WAITING_HOST_CHARGE.canApproveFromHostCharge());
        assertFalse(ReferralCommissionStatus.PENDING.canApproveFromHostCharge());
        assertFalse(ReferralCommissionStatus.APPROVED.canApproveFromHostCharge());
        assertFalse(ReferralCommissionStatus.PAID.canApproveFromHostCharge());
        assertFalse(ReferralCommissionStatus.CANCELLED.canApproveFromHostCharge());
        assertFalse(ReferralCommissionStatus.WITHHELD.canApproveFromHostCharge());
    }

    @Test
    @DisplayName("canCancel() should return true for WAITING_HOST_CHARGE")
    void canCancelShouldReturnTrueForWaitingHostCharge() {
        assertTrue(ReferralCommissionStatus.WAITING_HOST_CHARGE.canCancel(),
            "Commission in WAITING_HOST_CHARGE can be cancelled if booking is cancelled");
    }

    @Test
    @DisplayName("isReadyForApproval() should return false for commission in WAITING_HOST_CHARGE")
    void isReadyForApprovalShouldReturnFalseForWaitingHostCharge() {
        ReferralCommission commission = ReferralCommission.create(
            BOOKING_ID, AFFILIATE_ID, AFFILIATE_PLAN, amount, bookingFinishedAt
        );

        assertFalse(commission.isReadyForApproval(),
            "WAITING_HOST_CHARGE commission must not be approved via the dispute-period scheduler");
    }

    @Test
    @DisplayName("isTerminal() should return false for WAITING_HOST_CHARGE")
    void isTerminalShouldReturnFalseForWaitingHostCharge() {
        assertFalse(ReferralCommissionStatus.WAITING_HOST_CHARGE.isTerminal(),
            "WAITING_HOST_CHARGE is not a terminal state");
    }

    @Test
    @DisplayName("isTerminal() should return false for WITHHELD (not terminal, can recover)")
    void isTerminalShouldReturnFalseForWithheld() {
        assertFalse(ReferralCommissionStatus.WITHHELD.isTerminal(),
            "WITHHELD is not a terminal state - can be re-reviewed");
    }

    @Test
    @DisplayName("canCancel() should allow cancellation from WAITING_HOST_CHARGE, PENDING, and APPROVED")
    void canCancelShouldAllowCancellationFromExpectedStatuses() {
        assertTrue(ReferralCommissionStatus.WAITING_HOST_CHARGE.canCancel());
        assertTrue(ReferralCommissionStatus.PENDING.canCancel());
        assertTrue(ReferralCommissionStatus.APPROVED.canCancel());
        assertFalse(ReferralCommissionStatus.PAID.canCancel());
        assertFalse(ReferralCommissionStatus.WITHHELD.canCancel());
        assertFalse(ReferralCommissionStatus.CANCELLED.canCancel());
    }

    // ========== HELPER METHODS ==========

    /**
     * Builds a ReferralCommission reconstituted with a specific status (for testing state guard methods).
     */
    private ReferralCommission buildCommissionInStatus(ReferralCommissionStatus status) {
        DisputePeriod disputePeriod = DisputePeriod.calculate(bookingFinishedAt);
        PaymentSchedule paymentSchedule = PaymentSchedule.calculate(
            disputePeriod.getDisputePeriodEndsAt()
        );

        return ReferralCommission.reconstitute(
            1L,
            BOOKING_ID,
            AFFILIATE_ID,
            AFFILIATE_PLAN,
            amount,
            paymentSchedule,
            disputePeriod,
            null,
            null,
            status,
            null,
            null,
            0,
            null,
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now()
        );
    }
}
