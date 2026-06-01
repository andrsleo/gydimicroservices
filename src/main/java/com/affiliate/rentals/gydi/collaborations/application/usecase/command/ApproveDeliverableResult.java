package com.affiliate.rentals.gydi.collaborations.application.usecase.command;

import com.affiliate.rentals.gydi.collaborations.domain.model.enums.AgreementStatus;

/**
 * Result of approving a deliverable.
 */
public record ApproveDeliverableResult(
        Long deliverableId,
        String deliverableStatus,
        AgreementStatus agreementStatus
) {}
