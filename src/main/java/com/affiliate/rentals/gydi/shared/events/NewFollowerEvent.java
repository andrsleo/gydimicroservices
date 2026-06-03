package com.affiliate.rentals.gydi.shared.events;

/**
 * Domain event published when a user follows another user.
 * Consumed by the notifications bounded context.
 */
public record NewFollowerEvent(
    Long followedUserId,   // receives notification
    Long followerUserId    // who started following
) {}
