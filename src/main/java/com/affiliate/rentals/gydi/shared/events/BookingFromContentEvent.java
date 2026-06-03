package com.affiliate.rentals.gydi.shared.events;

/**
 * Domain event published when a booking is created via a content post attribution.
 * Consumed by the notifications bounded context.
 */
public record BookingFromContentEvent(
    Long bookingId,
    Long contentPostId,
    Long creatorUserId,   // creator who made the content — receives notification
    Long propertyId
) {}
