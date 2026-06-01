package com.affiliate.rentals.gydi.collaborations.domain.model;

import com.affiliate.rentals.gydi.collaborations.domain.model.enums.DeliverableType;

import java.util.Objects;

/**
 * Value object representing a deliverable proposed in a pitch.
 * Pure Java — no framework annotations.
 */
public final class PitchDeliverable {

    private final Long id;
    private final DeliverableType type;
    private final int quantity;
    private final String notes;

    private PitchDeliverable(Long id, DeliverableType type, int quantity, String notes) {
        this.id = id;
        this.type = Objects.requireNonNull(type, "type must not be null");
        if (quantity <= 0) throw new IllegalArgumentException("quantity must be > 0");
        this.quantity = quantity;
        this.notes = notes;
    }

    public static PitchDeliverable of(DeliverableType type, int quantity, String notes) {
        return new PitchDeliverable(null, type, quantity, notes);
    }

    public static PitchDeliverable reconstitute(Long id, DeliverableType type, int quantity, String notes) {
        return new PitchDeliverable(id, type, quantity, notes);
    }

    public Long id() { return id; }
    public DeliverableType type() { return type; }
    public int quantity() { return quantity; }
    public String notes() { return notes; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PitchDeliverable that)) return false;
        return quantity == that.quantity && type == that.type && Objects.equals(notes, that.notes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, quantity, notes);
    }
}
