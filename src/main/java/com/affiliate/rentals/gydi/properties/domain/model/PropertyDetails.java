package com.affiliate.rentals.gydi.properties.domain.model;

import java.util.Objects;

/**
 * PropertyDetails value object representing physical characteristics of a rental property.
 *
 * <p>This is an immutable value object following Domain-Driven Design (DDD) principles.
 * It encapsulates the physical attributes of a property including the number of bedrooms,
 * bathrooms, beds, and guest capacity. These details are crucial for property search and
 * filtering capabilities.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * PropertyDetails details = PropertyDetails.of(3, 2, 4, 6);
 * boolean canFit = details.canAccommodate(4);
 * PropertyDetails minimalDetails = PropertyDetails.withCapacity(2);
 * }</pre>
 *
 * @param bedrooms the number of bedrooms, may be {@code null}
 * @param bathrooms the number of bathrooms, may be {@code null}
 * @param beds the number of beds, may be {@code null}
 * @param capacity the maximum guest capacity, never {@code null} and must be positive
 * @author GYDI Development Team
 */
public record PropertyDetails(
        Integer bedrooms,
        Integer bathrooms,
        Integer beds,
        Integer capacity
) {

    /**
     * Compact constructor that validates property details.
     * Ensures capacity is positive and all counts are non-negative.
     *
     * @throws NullPointerException if capacity is {@code null}
     * @throws IllegalArgumentException if capacity is not positive or any count is negative
     */
    public PropertyDetails {
        Objects.requireNonNull(capacity, "Capacity cannot be null");
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be greater than 0");
        }
        if (bedrooms != null && bedrooms < 0) {
            throw new IllegalArgumentException("Bedrooms cannot be negative");
        }
        if (bathrooms != null && bathrooms < 0) {
            throw new IllegalArgumentException("Bathrooms cannot be negative");
        }
        if (beds != null && beds < 0) {
            throw new IllegalArgumentException("Beds cannot be negative");
        }
    }

    /**
     * Factory method to create PropertyDetails with all attributes specified.
     *
     * @param bedrooms the number of bedrooms
     * @param bathrooms the number of bathrooms
     * @param beds the number of beds
     * @param capacity the maximum guest capacity
     * @return a new PropertyDetails instance
     * @throws NullPointerException if capacity is {@code null}
     * @throws IllegalArgumentException if capacity is not positive or any count is negative
     */
    public static PropertyDetails of(Integer bedrooms, Integer bathrooms, Integer beds, Integer capacity) {
        return new PropertyDetails(bedrooms, bathrooms, beds, capacity);
    }

    /**
     * Factory method to create minimal PropertyDetails with only capacity specified.
     * Useful when detailed property information is not yet available.
     *
     * @param capacity the maximum guest capacity
     * @return a new PropertyDetails instance with null bedrooms, bathrooms, and beds
     * @throws NullPointerException if capacity is {@code null}
     * @throws IllegalArgumentException if capacity is not positive
     */
    public static PropertyDetails withCapacity(Integer capacity) {
        return new PropertyDetails(null, null, null, capacity);
    }

    /**
     * Checks if the property can accommodate a specified number of guests.
     *
     * @param numberOfGuests the number of guests to check
     * @return {@code true} if the property can accommodate the guests, {@code false} otherwise
     */
    public boolean canAccommodate(int numberOfGuests) {
        return capacity >= numberOfGuests;
    }

    @Override
    public String toString() {
        return "PropertyDetails[bedrooms=%d, bathrooms=%d, beds=%d, capacity=%d]"
                .formatted(bedrooms, bathrooms, beds, capacity);
    }
}
