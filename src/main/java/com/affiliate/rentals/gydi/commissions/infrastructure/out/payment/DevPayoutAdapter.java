package com.affiliate.rentals.gydi.commissions.infrastructure.out.payment;

import com.affiliate.rentals.gydi.commissions.domain.ports.PayoutGatewayPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Development/local fallback for {@link PayoutGatewayPort}.
 * <p>
 * Active in all non-prod profiles (local, dev, test).
 * Logs the payout request and returns a mock success result
 * without making any real HTTP call to PayPal.
 * </p>
 */
@Component
@Profile("!prod")
public class DevPayoutAdapter implements PayoutGatewayPort {

    private static final Logger logger = LoggerFactory.getLogger(DevPayoutAdapter.class);

    @Override
    public PayoutResult sendPayout(
            String recipientPayPalEmail,
            BigDecimal amount,
            String currency,
            String referenceId,
            String note
    ) {
        String mockBatchId = "DEV-BATCH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String mockItemId  = "DEV-ITEM-"  + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        logger.info("[DEV] PayPal payout SIMULATED: recipient={}, amount={} {}, referenceId={}, note={}",
                recipientPayPalEmail, amount, currency, referenceId, note);
        logger.info("[DEV] Mock PayPal response: batchId={}, itemId={}, status=PENDING",
                mockBatchId, mockItemId);

        return PayoutResult.success(mockBatchId, mockItemId, "PENDING");
    }
}
