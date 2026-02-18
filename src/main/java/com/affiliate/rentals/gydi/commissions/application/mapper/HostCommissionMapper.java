package com.affiliate.rentals.gydi.commissions.application.mapper;

import com.affiliate.rentals.gydi.commissions.application.dto.HostCommissionDto;
import com.affiliate.rentals.gydi.commissions.domain.model.HostCommission;
import org.springframework.stereotype.Component;

/**
 * Mapper for HostCommission domain model to DTO.
 */
@Component
public class HostCommissionMapper {

    public HostCommissionDto toDto(HostCommission domain) {
        if (domain == null) {
            return null;
        }

        return new HostCommissionDto(
            domain.getId(),
            domain.getBookingId(),
            domain.getHostId(),
            domain.getHostPlan(),
            domain.getAmount().getBookingAmount(),
            domain.getAmount().getCommissionRate(),
            domain.getAmount().getCommissionAmount(),
            domain.getAmount().getCurrency(),
            domain.getStatus().name(),
            domain.getStripePaymentIntentId(),
            domain.getStripeChargeId(),
            domain.getChargedAt(),
            domain.getFailureReason(),
            domain.getAttemptCount(),
            domain.getCreatedAt(),
            domain.getUpdatedAt()
        );
    }
}
