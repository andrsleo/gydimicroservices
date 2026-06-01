package com.affiliate.rentals.gydi.collaborations.infrastructure.in.rest.dto;

import com.affiliate.rentals.gydi.collaborations.domain.model.CounterOfferDeliverable;
import com.affiliate.rentals.gydi.collaborations.domain.model.enums.CompensationType;
import com.affiliate.rentals.gydi.collaborations.domain.model.enums.OfferedBy;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record CreateCounterOfferRequest(
        @NotNull OfferedBy offeredBy,
        @Size(max = 500) String message,
        @NotNull CompensationType compensationType,
        Integer nights,
        BigDecimal amount,
        String currency,
        BigDecimal bookingCommissionPct,
        List<String> experienceItems,
        List<CounterOfferDeliverable> deliverables
) {}
