package com.affiliate.rentals.gydi.referrals.application.dto;

public record ResolveReferralResponse(
        String destinationUrl,
        Long propertyId,
        Long affiliateId) {
}
