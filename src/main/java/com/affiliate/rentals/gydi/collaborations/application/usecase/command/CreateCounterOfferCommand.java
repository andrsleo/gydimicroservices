package com.affiliate.rentals.gydi.collaborations.application.usecase.command;

import com.affiliate.rentals.gydi.collaborations.domain.model.CounterOfferDeliverable;
import com.affiliate.rentals.gydi.collaborations.domain.model.enums.CompensationType;
import com.affiliate.rentals.gydi.collaborations.domain.model.enums.OfferedBy;

import java.math.BigDecimal;
import java.util.List;

/**
 * Command to send a counter-offer during pitch negotiation.
 *
 * @author GYDI Development Team
 */
public record CreateCounterOfferCommand(
        Long pitchId,
        Long requestingUserId,
        OfferedBy offeredBy,
        String message,
        CompensationType compensationType,
        Integer nights,
        BigDecimal amount,
        String currency,
        BigDecimal bookingCommissionPct,
        List<String> experienceItems,
        List<CounterOfferDeliverable> deliverables
) {}
