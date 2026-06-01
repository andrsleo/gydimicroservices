package com.affiliate.rentals.gydi.collaborations.domain.model;

import com.affiliate.rentals.gydi.collaborations.domain.model.enums.OfferedBy;
import com.affiliate.rentals.gydi.collaborations.domain.model.enums.PitchStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Aggregate root for the pitch/negotiation flow.
 *
 * <p>State machine:
 * PENDING → ACCEPTED | DECLINED | EXPIRED | CANCELLED | COUNTERED
 * COUNTERED → ACCEPTED | DECLINED | EXPIRED | CANCELLED | COUNTERED (max 3 rounds)
 * ACCEPTED → COMPLETED (when agreement completes)
 *
 * <p>Counter-offer cap: max 3 rounds total. After round 3, only accept/decline allowed.
 * Expiration clock resets 7 days from most recent action (including each counter-offer sent).
 *
 * <p>Pure Java — no Spring, JPA, or Lombok annotations.</p>
 */
public final class CreatorPitch {

    private final Long id;
    private final Long propertyId;
    private final Long hostId;
    private final Long creatorId;
    private final String introduction;
    private final String portfolioUrl;
    private PitchStatus status;
    private int counterOfferRounds;
    private final LocalDate preferredCheckIn;
    private final LocalDate preferredCheckOut;
    private OffsetDateTime expiresAt;
    private String declinedReason;
    private final List<PitchDeliverable> deliverables;
    private final PitchCompensation compensation;
    private final List<CounterOffer> counterOffers;
    private final OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    private static final int MAX_COUNTER_OFFER_ROUNDS = 3;
    private static final int EXPIRATION_DAYS = 7;

    private CreatorPitch(Builder builder) {
        this.id = builder.id;
        this.propertyId = Objects.requireNonNull(builder.propertyId, "propertyId must not be null");
        this.hostId = Objects.requireNonNull(builder.hostId, "hostId must not be null");
        this.creatorId = Objects.requireNonNull(builder.creatorId, "creatorId must not be null");
        this.introduction = Objects.requireNonNull(builder.introduction, "introduction must not be null");
        if (this.introduction.length() > 1000)
            throw new IllegalArgumentException("introduction must not exceed 1000 characters");
        this.portfolioUrl = builder.portfolioUrl;
        this.status = Objects.requireNonNullElse(builder.status, PitchStatus.PENDING);
        this.counterOfferRounds = builder.counterOfferRounds;
        this.preferredCheckIn = Objects.requireNonNull(builder.preferredCheckIn, "preferredCheckIn must not be null");
        this.preferredCheckOut = Objects.requireNonNull(builder.preferredCheckOut, "preferredCheckOut must not be null");
        if (!preferredCheckOut.isAfter(preferredCheckIn))
            throw new IllegalArgumentException("checkOut must be after checkIn");
        this.expiresAt = Objects.requireNonNullElseGet(builder.expiresAt,
                () -> OffsetDateTime.now().plusDays(EXPIRATION_DAYS));
        this.declinedReason = builder.declinedReason;
        this.deliverables = builder.deliverables != null ? new ArrayList<>(builder.deliverables) : new ArrayList<>();
        this.compensation = Objects.requireNonNull(builder.compensation, "compensation must not be null");
        this.counterOffers = builder.counterOffers != null ? new ArrayList<>(builder.counterOffers) : new ArrayList<>();
        this.createdAt = Objects.requireNonNullElseGet(builder.createdAt, OffsetDateTime::now);
        this.updatedAt = Objects.requireNonNullElseGet(builder.updatedAt, OffsetDateTime::now);
    }

    /**
     * Factory: create a new pitch. Sets status=PENDING and expiresAt=now+7d.
     */
    public static CreatorPitch create(Long propertyId, Long hostId, Long creatorId,
                                      String introduction, String portfolioUrl,
                                      LocalDate preferredCheckIn, LocalDate preferredCheckOut,
                                      List<PitchDeliverable> deliverables, PitchCompensation compensation) {
        return new Builder()
                .propertyId(propertyId)
                .hostId(hostId)
                .creatorId(creatorId)
                .introduction(introduction)
                .portfolioUrl(portfolioUrl)
                .preferredCheckIn(preferredCheckIn)
                .preferredCheckOut(preferredCheckOut)
                .status(PitchStatus.PENDING)
                .counterOfferRounds(0)
                .deliverables(deliverables)
                .compensation(compensation)
                .expiresAt(OffsetDateTime.now().plusDays(EXPIRATION_DAYS))
                .build();
    }

    // ── State transitions ──────────────────────────────────────────────────────

    /** Host or creator accepts the current offer. */
    public void accept() {
        requireStatus(PitchStatus.PENDING, PitchStatus.COUNTERED);
        this.status = PitchStatus.ACCEPTED;
        this.updatedAt = OffsetDateTime.now();
    }

    /** Host or creator declines with optional reason. */
    public void decline(String reason) {
        requireStatus(PitchStatus.PENDING, PitchStatus.COUNTERED);
        this.status = PitchStatus.DECLINED;
        this.declinedReason = reason;
        this.updatedAt = OffsetDateTime.now();
    }

    /** Creator withdraws the pitch. */
    public void cancel() {
        requireStatus(PitchStatus.PENDING, PitchStatus.COUNTERED);
        this.status = PitchStatus.CANCELLED;
        this.updatedAt = OffsetDateTime.now();
    }

    /** System marks pitch as expired (7-day timeout). */
    public void expire() {
        requireStatus(PitchStatus.PENDING, PitchStatus.COUNTERED);
        this.status = PitchStatus.EXPIRED;
        this.updatedAt = OffsetDateTime.now();
    }

    /** Agreement completed — mark pitch COMPLETED. */
    public void complete() {
        requireStatus(PitchStatus.ACCEPTED);
        this.status = PitchStatus.COMPLETED;
        this.updatedAt = OffsetDateTime.now();
    }

    /**
     * Adds a counter-offer round. Resets expiration clock by 7 days.
     *
     * @throws IllegalStateException if max rounds (3) already reached
     * @throws IllegalStateException if pitch not in PENDING or COUNTERED status
     */
    public void addCounterOffer(CounterOffer counterOffer) {
        requireStatus(PitchStatus.PENDING, PitchStatus.COUNTERED);
        if (this.counterOfferRounds >= MAX_COUNTER_OFFER_ROUNDS) {
            throw new IllegalStateException(
                    "Maximum negotiation rounds reached. Only accept or decline is allowed.");
        }
        this.counterOffers.add(counterOffer);
        this.counterOfferRounds++;
        this.status = PitchStatus.COUNTERED;
        // Reset expiration clock from now
        this.expiresAt = OffsetDateTime.now().plusDays(EXPIRATION_DAYS);
        this.updatedAt = OffsetDateTime.now();
    }

    /** Whether another counter-offer can be sent (rounds < 3 and status allows it). */
    public boolean canCounterOffer() {
        return (this.status == PitchStatus.PENDING || this.status == PitchStatus.COUNTERED)
                && this.counterOfferRounds < MAX_COUNTER_OFFER_ROUNDS;
    }

    /** Who should respond next (HOST if last offer was by CREATOR, vice versa). */
    public OfferedBy nextResponder() {
        if (counterOffers.isEmpty()) return OfferedBy.HOST;
        return counterOffers.getLast().offeredBy() == OfferedBy.HOST ? OfferedBy.CREATOR : OfferedBy.HOST;
    }

    // ── Accessors ──────────────────────────────────────────────────────────────

    public Long id() { return id; }
    public Long propertyId() { return propertyId; }
    public Long hostId() { return hostId; }
    public Long creatorId() { return creatorId; }
    public String introduction() { return introduction; }
    public String portfolioUrl() { return portfolioUrl; }
    public PitchStatus status() { return status; }
    public int counterOfferRounds() { return counterOfferRounds; }
    public LocalDate preferredCheckIn() { return preferredCheckIn; }
    public LocalDate preferredCheckOut() { return preferredCheckOut; }
    public OffsetDateTime expiresAt() { return expiresAt; }
    public String declinedReason() { return declinedReason; }
    public List<PitchDeliverable> deliverables() { return List.copyOf(deliverables); }
    public PitchCompensation compensation() { return compensation; }
    public List<CounterOffer> counterOffers() { return List.copyOf(counterOffers); }
    public OffsetDateTime createdAt() { return createdAt; }
    public OffsetDateTime updatedAt() { return updatedAt; }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private void requireStatus(PitchStatus... allowed) {
        for (PitchStatus s : allowed) {
            if (this.status == s) return;
        }
        throw new IllegalStateException(
                "Invalid transition from status " + this.status + ". Allowed: " + List.of(allowed));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CreatorPitch that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "CreatorPitch[id=%s, propertyId=%s, creatorId=%s, status=%s, rounds=%d]"
                .formatted(id, propertyId, creatorId, status, counterOfferRounds);
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Long id;
        private Long propertyId;
        private Long hostId;
        private Long creatorId;
        private String introduction;
        private String portfolioUrl;
        private PitchStatus status;
        private int counterOfferRounds;
        private LocalDate preferredCheckIn;
        private LocalDate preferredCheckOut;
        private OffsetDateTime expiresAt;
        private String declinedReason;
        private List<PitchDeliverable> deliverables;
        private PitchCompensation compensation;
        private List<CounterOffer> counterOffers;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;

        private Builder() {}

        public Builder id(Long id) { this.id = id; return this; }
        public Builder propertyId(Long propertyId) { this.propertyId = propertyId; return this; }
        public Builder hostId(Long hostId) { this.hostId = hostId; return this; }
        public Builder creatorId(Long creatorId) { this.creatorId = creatorId; return this; }
        public Builder introduction(String introduction) { this.introduction = introduction; return this; }
        public Builder portfolioUrl(String portfolioUrl) { this.portfolioUrl = portfolioUrl; return this; }
        public Builder status(PitchStatus status) { this.status = status; return this; }
        public Builder counterOfferRounds(int counterOfferRounds) { this.counterOfferRounds = counterOfferRounds; return this; }
        public Builder preferredCheckIn(LocalDate preferredCheckIn) { this.preferredCheckIn = preferredCheckIn; return this; }
        public Builder preferredCheckOut(LocalDate preferredCheckOut) { this.preferredCheckOut = preferredCheckOut; return this; }
        public Builder expiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; return this; }
        public Builder declinedReason(String declinedReason) { this.declinedReason = declinedReason; return this; }
        public Builder deliverables(List<PitchDeliverable> deliverables) { this.deliverables = deliverables; return this; }
        public Builder compensation(PitchCompensation compensation) { this.compensation = compensation; return this; }
        public Builder counterOffers(List<CounterOffer> counterOffers) { this.counterOffers = counterOffers; return this; }
        public Builder createdAt(OffsetDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public CreatorPitch build() { return new CreatorPitch(this); }
    }
}
