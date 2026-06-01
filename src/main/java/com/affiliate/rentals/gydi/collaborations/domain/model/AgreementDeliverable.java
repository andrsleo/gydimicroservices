package com.affiliate.rentals.gydi.collaborations.domain.model;

import com.affiliate.rentals.gydi.collaborations.domain.model.enums.DeliverableType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Entity representing an agreed deliverable within a collaboration agreement.
 * Tracks its own review status and associated uploaded assets.
 * Pure Java — no framework annotations.
 */
public final class AgreementDeliverable {

    private final Long id;
    private final Long agreementId;
    private final DeliverableType type;
    private final int quantity;
    private String status;      // PENDING | SUBMITTED | APPROVED | REVISION_REQUESTED
    private String revisionFeedback;
    private final List<DeliveryAsset> assets;

    private AgreementDeliverable(Long id, Long agreementId, DeliverableType type, int quantity,
                                  String status, String revisionFeedback, List<DeliveryAsset> assets) {
        this.id = id;
        this.agreementId = Objects.requireNonNull(agreementId, "agreementId must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        if (quantity <= 0) throw new IllegalArgumentException("quantity must be > 0");
        this.quantity = quantity;
        this.status = Objects.requireNonNullElse(status, "PENDING");
        this.revisionFeedback = revisionFeedback;
        this.assets = assets != null ? new ArrayList<>(assets) : new ArrayList<>();
    }

    public static AgreementDeliverable create(Long agreementId, DeliverableType type, int quantity) {
        return new AgreementDeliverable(null, agreementId, type, quantity, "PENDING", null, null);
    }

    public static AgreementDeliverable reconstitute(Long id, Long agreementId, DeliverableType type,
                                                     int quantity, String status, String revisionFeedback,
                                                     List<DeliveryAsset> assets) {
        return new AgreementDeliverable(id, agreementId, type, quantity, status, revisionFeedback, assets);
    }

    public void markSubmitted() {
        this.status = "SUBMITTED";
    }

    public void approve() {
        this.status = "APPROVED";
        this.revisionFeedback = null;
    }

    public void requestRevision(String feedback) {
        Objects.requireNonNull(feedback, "feedback must not be null");
        this.status = "REVISION_REQUESTED";
        this.revisionFeedback = feedback;
    }

    public void addAsset(DeliveryAsset asset) {
        this.assets.add(asset);
        if ("PENDING".equals(this.status) || "REVISION_REQUESTED".equals(this.status)) {
            this.status = "SUBMITTED";
        }
    }

    public boolean isApproved() {
        return "APPROVED".equals(this.status);
    }

    public Long id() { return id; }
    public Long agreementId() { return agreementId; }
    public DeliverableType type() { return type; }
    public int quantity() { return quantity; }
    public String status() { return status; }
    public String revisionFeedback() { return revisionFeedback; }
    public List<DeliveryAsset> assets() { return List.copyOf(assets); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AgreementDeliverable that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
