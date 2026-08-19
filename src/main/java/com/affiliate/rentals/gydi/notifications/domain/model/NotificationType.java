package com.affiliate.rentals.gydi.notifications.domain.model;

public enum NotificationType {
    NEW_LIKE,
    NEW_FOLLOWER,
    BOOKING_FROM_CONTENT,
    CONTENT_MILESTONE,

    // Collaboration marketplace notifications
    PITCH_RECEIVED,
    PITCH_ACCEPTED,
    PITCH_DECLINED,
    COUNTER_OFFER_RECEIVED,
    DELIVERY_SUBMITTED,
    DELIVERY_APPROVED,
    REVISION_REQUESTED,
    PITCH_EXPIRED,
    AGREEMENT_CANCELLED
}
