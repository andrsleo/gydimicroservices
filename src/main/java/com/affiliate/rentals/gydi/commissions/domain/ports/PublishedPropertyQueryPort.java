package com.affiliate.rentals.gydi.commissions.domain.ports;

/**
 * Port to query published property status for affiliate commission rate calculation.
 * <p>
 * Cross-context query: commissions context reads from properties context.
 * Used to determine if an affiliate earns 6% (has published property) or 4% (default).
 * </p>
 */
public interface PublishedPropertyQueryPort {

    /**
     * Returns true if the given user has at least one property with status PUBLISHED.
     *
     * @param userId the user ID to check
     * @return true if the user has a published property, false otherwise
     */
    boolean hasPublishedProperty(Long userId);
}
