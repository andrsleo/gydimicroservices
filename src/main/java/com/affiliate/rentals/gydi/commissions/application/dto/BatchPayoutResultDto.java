package com.affiliate.rentals.gydi.commissions.application.dto;

/**
 * DTO for batch payout result.
 *
 * @param successfulPayouts Number of successful payouts
 * @param failedPayouts Number of failed payouts
 * @param totalAmountPaid Total amount paid (in cents)
 * @param currency Currency code
 */
public record BatchPayoutResultDto(
    int successfulPayouts,
    int failedPayouts,
    long totalAmountPaid,
    String currency
) {
    public static BatchPayoutResultDto empty() {
        return new BatchPayoutResultDto(0, 0, 0L, "USD");
    }
}
