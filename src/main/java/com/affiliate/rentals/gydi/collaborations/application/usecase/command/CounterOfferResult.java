package com.affiliate.rentals.gydi.collaborations.application.usecase.command;

import java.time.OffsetDateTime;

/**
 * Result returned after creating a counter-offer.
 *
 * @author GYDI Development Team
 */
public record CounterOfferResult(
        Long pitchId,
        Long counterOfferId,
        int roundNumber,
        String status,
        boolean canCounterOfferAgain,
        OffsetDateTime expiresAt
) {}
