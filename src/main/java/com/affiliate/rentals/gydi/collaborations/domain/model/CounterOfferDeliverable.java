package com.affiliate.rentals.gydi.collaborations.domain.model;

import com.affiliate.rentals.gydi.collaborations.domain.model.enums.DeliverableType;

import java.util.Objects;

/**
 * Value object representing a deliverable modification within a counter-offer.
 * Pure Java — no framework annotations.
 */
public final class CounterOfferDeliverable {

    private final Long id;
    private final DeliverableType type;
    private final int quantity;
    private final String notes;

    private CounterOfferDeliverable(Long id, DeliverableType type, int quantity, String notes) {
        this.id = id;
        this.type = Objects.requireNonNull(type, "type must not be null");
        if (quantity <= 0) throw new IllegalArgumentException("quantity must be > 0");
        this.quantity = quantity;
        this.notes = notes;
    }

    public static CounterOfferDeliverable of(DeliverableType type, int quantity, String notes) {
        return new CounterOfferDeliverable(null, type, quantity, notes);
    }

    public static CounterOfferDeliverable reconstitute(Long id, DeliverableType type, int quantity, String notes) {
        return new CounterOfferDeliverable(id, type, quantity, notes);
    }

    public Long id() { return id; }
    public DeliverableType type() { return type; }
    public int quantity() { return quantity; }
    public String notes() { return notes; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CounterOfferDeliverable that)) return false;
        return quantity == that.quantity && type == that.type && Objects.equals(notes, that.notes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, quantity, notes);
    }
}
