package com.affiliate.rentals.gydi.collaborations.domain.model;

import com.affiliate.rentals.gydi.collaborations.domain.model.enums.AgreementStatus;
import com.affiliate.rentals.gydi.collaborations.domain.model.enums.OfferedBy;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Aggregate root for the collaboration agreement.
 *
 * <p>State machine:
 * ACTIVE → IN_PROGRESS (check-in date reached)
 * IN_PROGRESS → DELIVERED (all deliverables submitted)
 * DELIVERED → COMPLETED (all deliverables approved)
 * DELIVERED → DELIVERED (revision requested → creator re-uploads)
 * ACTIVE → CANCELLED (either party cancels before check-in)
 *
 * <p>Auto-generated when a pitch is ACCEPTED. Stores agreed terms as a structured DB record.
 * Pure Java — no Spring, JPA, or Lombok annotations.</p>
 */
public final class CollaborationAgreement {

    private static final int DELIVERY_DEADLINE_DAYS = 14;
    private static final int POSTING_DEADLINE_DAYS = 7;
    private static final String DEFAULT_CANCELLATION_POLICY =
            "Either party may cancel up to 48 hours before check-in.";

    private final Long id;
    private final Long pitchId;
    private final Long propertyId;
    private final Long hostId;
    private final Long creatorId;
    private AgreementStatus status;
    private final LocalDate checkInDate;
    private final LocalDate checkOutDate;
    private final LocalDate deliveryDeadline;
    private final LocalDate postingDeadline;
    private final String cancellationPolicy;
    private OfferedBy cancelledBy;
    private OffsetDateTime cancelledAt;
    private final List<AgreementDeliverable> deliverables;
    private final List<ContentRight> contentRights;
    private final PitchCompensation compensation;
    private final OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    private CollaborationAgreement(Builder builder) {
        this.id = builder.id;
        this.pitchId = Objects.requireNonNull(builder.pitchId, "pitchId must not be null");
        this.propertyId = Objects.requireNonNull(builder.propertyId, "propertyId must not be null");
        this.hostId = Objects.requireNonNull(builder.hostId, "hostId must not be null");
        this.creatorId = Objects.requireNonNull(builder.creatorId, "creatorId must not be null");
        this.status = Objects.requireNonNullElse(builder.status, AgreementStatus.ACTIVE);
        this.checkInDate = Objects.requireNonNull(builder.checkInDate, "checkInDate must not be null");
        this.checkOutDate = Objects.requireNonNull(builder.checkOutDate, "checkOutDate must not be null");
        this.deliveryDeadline = Objects.requireNonNullElseGet(builder.deliveryDeadline,
                () -> checkOutDate.plusDays(DELIVERY_DEADLINE_DAYS));
        this.postingDeadline = Objects.requireNonNullElseGet(builder.postingDeadline,
                () -> this.deliveryDeadline.plusDays(POSTING_DEADLINE_DAYS));
        this.cancellationPolicy = Objects.requireNonNullElse(builder.cancellationPolicy, DEFAULT_CANCELLATION_POLICY);
        this.cancelledBy = builder.cancelledBy;
        this.cancelledAt = builder.cancelledAt;
        this.deliverables = builder.deliverables != null ? new ArrayList<>(builder.deliverables) : new ArrayList<>();
        this.contentRights = builder.contentRights != null ? new ArrayList<>(builder.contentRights) : new ArrayList<>();
        this.compensation = Objects.requireNonNull(builder.compensation, "compensation must not be null");
        this.createdAt = Objects.requireNonNullElseGet(builder.createdAt, OffsetDateTime::now);
        this.updatedAt = Objects.requireNonNullElseGet(builder.updatedAt, OffsetDateTime::now);
    }

    /**
     * Factory: create a new agreement from an accepted pitch.
     * deliveryDeadline = checkOut + 14 days. postingDeadline = deliveryDeadline + 7 days.
     */
    public static CollaborationAgreement fromPitch(Long pitchId, Long propertyId, Long hostId, Long creatorId,
                                                    LocalDate checkInDate, LocalDate checkOutDate,
                                                    List<AgreementDeliverable> deliverables,
                                                    List<ContentRight> contentRights,
                                                    PitchCompensation compensation) {
        return new Builder()
                .pitchId(pitchId)
                .propertyId(propertyId)
                .hostId(hostId)
                .creatorId(creatorId)
                .checkInDate(checkInDate)
                .checkOutDate(checkOutDate)
                .status(AgreementStatus.ACTIVE)
                .deliverables(deliverables)
                .contentRights(contentRights)
                .compensation(compensation)
                .build();
    }

    // ── State transitions ──────────────────────────────────────────────────────

    /** Marks agreement as IN_PROGRESS when creator checks in. */
    public void startProgress() {
        requireStatus(AgreementStatus.ACTIVE);
        this.status = AgreementStatus.IN_PROGRESS;
        this.updatedAt = OffsetDateTime.now();
    }

    /**
     * Marks agreement DELIVERED when all deliverables have been submitted.
     * Enforced by use case after all deliverables transition to SUBMITTED.
     */
    public void markDelivered() {
        requireStatus(AgreementStatus.IN_PROGRESS);
        this.status = AgreementStatus.DELIVERED;
        this.updatedAt = OffsetDateTime.now();
    }

    /**
     * Marks agreement COMPLETED when all deliverables are APPROVED.
     */
    public void complete() {
        requireStatus(AgreementStatus.DELIVERED);
        boolean allApproved = deliverables.stream().allMatch(AgreementDeliverable::isApproved);
        if (!allApproved)
            throw new IllegalStateException("Cannot complete: not all deliverables are approved");
        this.status = AgreementStatus.COMPLETED;
        this.updatedAt = OffsetDateTime.now();
    }

    /**
     * Cancels the agreement. Only allowed before check-in (ACTIVE status).
     */
    public void cancel(OfferedBy cancelledBy) {
        requireStatus(AgreementStatus.ACTIVE);
        this.status = AgreementStatus.CANCELLED;
        this.cancelledBy = cancelledBy;
        this.cancelledAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    /** Whether the agreement can still be cancelled (only before check-in). */
    public boolean canCancel() {
        return this.status == AgreementStatus.ACTIVE;
    }

    // ── Accessors ──────────────────────────────────────────────────────────────

    public Long id() { return id; }
    public Long pitchId() { return pitchId; }
    public Long propertyId() { return propertyId; }
    public Long hostId() { return hostId; }
    public Long creatorId() { return creatorId; }
    public AgreementStatus status() { return status; }
    public LocalDate checkInDate() { return checkInDate; }
    public LocalDate checkOutDate() { return checkOutDate; }
    public LocalDate deliveryDeadline() { return deliveryDeadline; }
    public LocalDate postingDeadline() { return postingDeadline; }
    public String cancellationPolicy() { return cancellationPolicy; }
    public OfferedBy cancelledBy() { return cancelledBy; }
    public OffsetDateTime cancelledAt() { return cancelledAt; }
    public List<AgreementDeliverable> deliverables() { return List.copyOf(deliverables); }
    public List<ContentRight> contentRights() { return List.copyOf(contentRights); }
    public PitchCompensation compensation() { return compensation; }
    public OffsetDateTime createdAt() { return createdAt; }
    public OffsetDateTime updatedAt() { return updatedAt; }

    private void requireStatus(AgreementStatus... allowed) {
        for (AgreementStatus s : allowed) {
            if (this.status == s) return;
        }
        throw new IllegalStateException(
                "Invalid transition from agreement status " + this.status + ". Allowed: " + List.of(allowed));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CollaborationAgreement that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "CollaborationAgreement[id=%s, pitchId=%s, status=%s]".formatted(id, pitchId, status);
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Long id;
        private Long pitchId;
        private Long propertyId;
        private Long hostId;
        private Long creatorId;
        private AgreementStatus status;
        private LocalDate checkInDate;
        private LocalDate checkOutDate;
        private LocalDate deliveryDeadline;
        private LocalDate postingDeadline;
        private String cancellationPolicy;
        private OfferedBy cancelledBy;
        private OffsetDateTime cancelledAt;
        private List<AgreementDeliverable> deliverables;
        private List<ContentRight> contentRights;
        private PitchCompensation compensation;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;

        private Builder() {}

        public Builder id(Long id) { this.id = id; return this; }
        public Builder pitchId(Long pitchId) { this.pitchId = pitchId; return this; }
        public Builder propertyId(Long propertyId) { this.propertyId = propertyId; return this; }
        public Builder hostId(Long hostId) { this.hostId = hostId; return this; }
        public Builder creatorId(Long creatorId) { this.creatorId = creatorId; return this; }
        public Builder status(AgreementStatus status) { this.status = status; return this; }
        public Builder checkInDate(LocalDate checkInDate) { this.checkInDate = checkInDate; return this; }
        public Builder checkOutDate(LocalDate checkOutDate) { this.checkOutDate = checkOutDate; return this; }
        public Builder deliveryDeadline(LocalDate deliveryDeadline) { this.deliveryDeadline = deliveryDeadline; return this; }
        public Builder postingDeadline(LocalDate postingDeadline) { this.postingDeadline = postingDeadline; return this; }
        public Builder cancellationPolicy(String cancellationPolicy) { this.cancellationPolicy = cancellationPolicy; return this; }
        public Builder cancelledBy(OfferedBy cancelledBy) { this.cancelledBy = cancelledBy; return this; }
        public Builder cancelledAt(OffsetDateTime cancelledAt) { this.cancelledAt = cancelledAt; return this; }
        public Builder deliverables(List<AgreementDeliverable> deliverables) { this.deliverables = deliverables; return this; }
        public Builder contentRights(List<ContentRight> contentRights) { this.contentRights = contentRights; return this; }
        public Builder compensation(PitchCompensation compensation) { this.compensation = compensation; return this; }
        public Builder createdAt(OffsetDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public CollaborationAgreement build() { return new CollaborationAgreement(this); }
    }
}
