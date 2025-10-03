package com.affiliate.rentals.gydi.bookings.domain.model;

import com.affiliate.rentals.gydi.properties.domain.model.Money;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Booking domain entity representing a property reservation.
 *
 * <p>This is an immutable domain entity following Domain-Driven Design (DDD) and SOLID principles.
 * It represents a complete booking/reservation for a rental property, including the guest information,
 * property details, date range, pricing, and booking status. The class uses value objects
 * ({@link DateRange}, {@link Money}, {@link BookingStatus}) for better domain modeling and type safety.</p>
 *
 * <p>The booking enforces business rules through validation and provides methods for status
 * transitions following a well-defined state machine. It maintains immutability by returning
 * new instances when state changes occur.</p>
 *
 * <p>Booking lifecycle:</p>
 * <ul>
 *   <li>Created in PENDING status</li>
 *   <li>Can be confirmed (CONFIRMED), cancelled (CANCELLED)</li>
 *   <li>Confirmed bookings can start (IN_PROGRESS)</li>
 *   <li>In-progress bookings can be completed (COMPLETED)</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * Booking booking = Booking.builder()
 *     .propertyId(1L)
 *     .userId(100L)
 *     .dateRange(LocalDate.now(), LocalDate.now().plusDays(3))
 *     .totalPrice(Money.of(300, "USD"))
 *     .build();
 *
 * Booking confirmed = booking.confirm();
 * Booking started = confirmed.startProgress();
 * long nights = booking.numberOfNights();
 * }</pre>
 *
 * @author GYDI Development Team
 * @see DateRange
 * @see BookingStatus
 * @see Money
 */
public final class Booking {
    private final Long id;
    private final Long propertyId;
    private final Long userId;
    private final DateRange dateRange;
    private final Money totalPrice;
    private final BookingStatus status;
    private final LocalDateTime createdAt;

    /**
     * Private constructor used by the Builder.
     * Validates all required fields and creates an immutable Booking instance.
     *
     * @param builder the builder instance containing all booking data
     * @throws NullPointerException if any required field is null
     * @throws IllegalArgumentException if total price is zero
     */
    private Booking(Builder builder) {
        this.id = builder.id;
        this.propertyId = Objects.requireNonNull(builder.propertyId, "Property ID cannot be null");
        this.userId = Objects.requireNonNull(builder.userId, "User ID cannot be null");
        this.dateRange = Objects.requireNonNull(builder.dateRange, "Date range cannot be null");
        this.totalPrice = Objects.requireNonNull(builder.totalPrice, "Total price cannot be null");
        this.status = Objects.requireNonNullElse(builder.status, BookingStatus.PENDING);
        this.createdAt = Objects.requireNonNullElseGet(builder.createdAt, LocalDateTime::now);

        validate();
    }

    /**
     * Validates business rules for the booking.
     *
     * @throws IllegalArgumentException if validation fails
     */
    private void validate() {
        if (totalPrice.isZero()) {
            throw new IllegalArgumentException("Total price must be greater than zero");
        }
    }

    /**
     * Returns the booking's unique identifier.
     *
     * @return the booking ID, may be {@code null} for new bookings not yet persisted
     */
    public Long id() {
        return id;
    }

    /**
     * Returns the ID of the property being booked.
     *
     * @return the property ID, never {@code null}
     */
    public Long propertyId() {
        return propertyId;
    }

    /**
     * Returns the ID of the user making the booking.
     *
     * @return the user ID, never {@code null}
     */
    public Long userId() {
        return userId;
    }

    /**
     * Returns the date range for this booking.
     *
     * @return the date range, never {@code null}
     */
    public DateRange dateRange() {
        return dateRange;
    }

    /**
     * Returns the booking's start date.
     * Convenience method that delegates to the date range.
     *
     * @return the start date, never {@code null}
     */
    public LocalDate startDate() {
        return dateRange.startDate();
    }

    /**
     * Returns the booking's end date.
     * Convenience method that delegates to the date range.
     *
     * @return the end date, never {@code null}
     */
    public LocalDate endDate() {
        return dateRange.endDate();
    }

    /**
     * Returns the total price for this booking.
     *
     * @return the total price, never {@code null} and never zero
     */
    public Money totalPrice() {
        return totalPrice;
    }

    /**
     * Returns the current booking status.
     *
     * @return the booking status, never {@code null}
     */
    public BookingStatus status() {
        return status;
    }

    /**
     * Returns the timestamp when this booking was created.
     *
     * @return the creation timestamp, never {@code null}
     */
    public LocalDateTime createdAt() {
        return createdAt;
    }

    /**
     * Calculates the number of nights in this booking.
     * Convenience method that delegates to the date range.
     *
     * @return the number of nights
     */
    public long numberOfNights() {
        return dateRange.numberOfNights();
    }

    /**
     * Checks if this booking is currently active.
     * Active bookings are those in CONFIRMED or IN_PROGRESS status.
     *
     * @return {@code true} if the booking is active, {@code false} otherwise
     */
    public boolean isActive() {
        return status.isActive();
    }

    /**
     * Checks if this booking is in PENDING status.
     *
     * @return {@code true} if pending, {@code false} otherwise
     */
    public boolean isPending() {
        return status == BookingStatus.PENDING;
    }

    /**
     * Checks if this booking is in CONFIRMED status.
     *
     * @return {@code true} if confirmed, {@code false} otherwise
     */
    public boolean isConfirmed() {
        return status == BookingStatus.CONFIRMED;
    }

    /**
     * Checks if this booking is in CANCELLED status.
     *
     * @return {@code true} if cancelled, {@code false} otherwise
     */
    public boolean isCancelled() {
        return status == BookingStatus.CANCELLED;
    }

    /**
     * Checks if this booking is in COMPLETED status.
     *
     * @return {@code true} if completed, {@code false} otherwise
     */
    public boolean isCompleted() {
        return status == BookingStatus.COMPLETED;
    }

    /**
     * Checks if this booking's date range overlaps with another booking.
     * Useful for detecting conflicting bookings.
     *
     * @param other the other booking to check against
     * @return {@code true} if the date ranges overlap, {@code false} otherwise
     */
    public boolean overlapsWith(Booking other) {
        return this.dateRange.overlaps(other.dateRange);
    }

    /**
     * Checks if this booking is for a specific property.
     *
     * @param propertyId the property ID to check
     * @return {@code true} if the booking is for the specified property, {@code false} otherwise
     */
    public boolean isForProperty(Long propertyId) {
        return this.propertyId.equals(propertyId);
    }

    /**
     * Checks if this booking was made by a specific user.
     *
     * @param userId the user ID to check
     * @return {@code true} if the booking belongs to the specified user, {@code false} otherwise
     */
    public boolean isForUser(Long userId) {
        return this.userId.equals(userId);
    }

    /**
     * Creates a new Booking instance with status changed to CONFIRMED.
     * This method maintains immutability by returning a new instance.
     *
     * @return a new Booking instance in CONFIRMED status
     * @throws IllegalStateException if the status transition is not allowed
     */
    public Booking confirm() {
        return changeStatus(BookingStatus.CONFIRMED);
    }

    /**
     * Creates a new Booking instance with status changed to CANCELLED.
     * This method maintains immutability by returning a new instance.
     *
     * @return a new Booking instance in CANCELLED status
     * @throws IllegalStateException if the status transition is not allowed
     */
    public Booking cancel() {
        return changeStatus(BookingStatus.CANCELLED);
    }

    /**
     * Creates a new Booking instance with status changed to COMPLETED.
     * This method maintains immutability by returning a new instance.
     *
     * @return a new Booking instance in COMPLETED status
     * @throws IllegalStateException if the status transition is not allowed
     */
    public Booking complete() {
        return changeStatus(BookingStatus.COMPLETED);
    }

    /**
     * Creates a new Booking instance with status changed to IN_PROGRESS.
     * Typically called when a guest checks in.
     * This method maintains immutability by returning a new instance.
     *
     * @return a new Booking instance in IN_PROGRESS status
     * @throws IllegalStateException if the status transition is not allowed
     */
    public Booking startProgress() {
        return changeStatus(BookingStatus.IN_PROGRESS);
    }

    /**
     * Creates a new Booking instance with a new status.
     * Validates that the status transition is allowed before making the change.
     * This method maintains immutability by returning a new instance.
     *
     * @param newStatus the new status to transition to
     * @return a new Booking instance with the new status
     * @throws IllegalStateException if the transition from current status to new status is not allowed
     */
    public Booking changeStatus(BookingStatus newStatus) {
        if (!this.status.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    "Cannot transition from %s to %s".formatted(this.status, newStatus)
            );
        }
        return this.toBuilder().status(newStatus).build();
    }

    /**
     * Calculates the price per night for this booking.
     * Divides the total price by the number of nights.
     *
     * @return the price per night
     */
    public Money pricePerNight() {
        return totalPrice.divide(numberOfNights());
    }

    /**
     * Creates a new Builder instance for constructing Booking objects.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a Builder instance initialized with this booking's current values.
     * Useful for creating modified copies of the booking.
     *
     * @return a Builder instance with all current values set
     */
    public Builder toBuilder() {
        return new Builder()
                .id(this.id)
                .propertyId(this.propertyId)
                .userId(this.userId)
                .dateRange(this.dateRange)
                .totalPrice(this.totalPrice)
                .status(this.status)
                .createdAt(this.createdAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Booking booking = (Booking) o;
        return Objects.equals(id, booking.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Booking[id=%d, propertyId=%d, userId=%d, dateRange=%s, status=%s, totalPrice=%s]"
                .formatted(id, propertyId, userId, dateRange, status, totalPrice);
    }

    /**
     * Builder class for constructing {@link Booking} instances.
     *
     * <p>This builder follows the fluent interface pattern and provides
     * methods for setting all booking attributes. It validates required fields
     * when {@link #build()} is called and sets sensible defaults for optional fields.</p>
     */
    public static final class Builder {
        private Long id;
        private Long propertyId;
        private Long userId;
        private DateRange dateRange;
        private Money totalPrice;
        private BookingStatus status;
        private LocalDateTime createdAt;

        private Builder() {
        }

        /**
         * Sets the booking ID.
         *
         * @param id the booking ID
         * @return this builder instance
         */
        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the property ID.
         *
         * @param propertyId the property ID
         * @return this builder instance
         */
        public Builder propertyId(Long propertyId) {
            this.propertyId = propertyId;
            return this;
        }

        /**
         * Sets the user ID.
         *
         * @param userId the user ID
         * @return this builder instance
         */
        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        /**
         * Sets the date range using a {@link DateRange} value object.
         *
         * @param dateRange the date range
         * @return this builder instance
         */
        public Builder dateRange(DateRange dateRange) {
            this.dateRange = dateRange;
            return this;
        }

        /**
         * Sets the date range using start and end dates.
         * The dates will be wrapped in a {@link DateRange} value object.
         *
         * @param startDate the start date
         * @param endDate the end date
         * @return this builder instance
         */
        public Builder dateRange(LocalDate startDate, LocalDate endDate) {
            this.dateRange = DateRange.of(startDate, endDate);
            return this;
        }

        /**
         * Sets the total price for the booking.
         *
         * @param totalPrice the total price
         * @return this builder instance
         */
        public Builder totalPrice(Money totalPrice) {
            this.totalPrice = totalPrice;
            return this;
        }

        /**
         * Sets the booking status.
         *
         * @param status the booking status
         * @return this builder instance
         */
        public Builder status(BookingStatus status) {
            this.status = status;
            return this;
        }

        /**
         * Sets the creation timestamp.
         *
         * @param createdAt the creation timestamp
         * @return this builder instance
         */
        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        /**
         * Builds and returns a new {@link Booking} instance.
         *
         * @return the constructed Booking
         * @throws NullPointerException if any required field is null
         * @throws IllegalArgumentException if total price is zero
         */
        public Booking build() {
            return new Booking(this);
        }
    }
}
