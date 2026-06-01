package com.affiliate.rentals.gydi.collaborations.domain.model;

import com.affiliate.rentals.gydi.collaborations.domain.model.enums.CompensationType;
import com.affiliate.rentals.gydi.collaborations.domain.model.enums.OfferedBy;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Entity representing a single counter-offer round in a negotiation.
 * Max 3 rounds per pitch. Pure Java — no framework annotations.
 */
public final class CounterOffer {

    private final Long id;
    private final Long pitchId;
    private final int roundNumber;
    private final OfferedBy offeredBy;
    private final String message;
    private final CompensationType compensationType;
    private final Integer nights;
    private final BigDecimal amount;
    private final String currency;
    private final BigDecimal bookingCommissionPct;
    private final List<String> experienceItems;
    private final List<CounterOfferDeliverable> deliverables;
    private final OffsetDateTime createdAt;

    private CounterOffer(Builder builder) {
        this.id = builder.id;
        this.pitchId = Objects.requireNonNull(builder.pitchId, "pitchId must not be null");
        if (builder.roundNumber < 1 || builder.roundNumber > 3)
            throw new IllegalArgumentException("roundNumber must be 1-3");
        this.roundNumber = builder.roundNumber;
        this.offeredBy = Objects.requireNonNull(builder.offeredBy, "offeredBy must not be null");
        this.message = builder.message;
        this.compensationType = builder.compensationType;
        this.nights = builder.nights;
        this.amount = builder.amount;
        this.currency = builder.currency;
        this.bookingCommissionPct = builder.bookingCommissionPct;
        this.experienceItems = builder.experienceItems != null ? List.copyOf(builder.experienceItems) : List.of();
        this.deliverables = builder.deliverables != null ? List.copyOf(builder.deliverables) : List.of();
        this.createdAt = Objects.requireNonNullElseGet(builder.createdAt, OffsetDateTime::now);
    }

    public static CounterOffer create(Long pitchId, int roundNumber, OfferedBy offeredBy,
                                      String message, CompensationType compensationType,
                                      Integer nights, BigDecimal amount, String currency,
                                      BigDecimal bookingCommissionPct, List<String> experienceItems,
                                      List<CounterOfferDeliverable> deliverables) {
        return new Builder()
                .pitchId(pitchId)
                .roundNumber(roundNumber)
                .offeredBy(offeredBy)
                .message(message)
                .compensationType(compensationType)
                .nights(nights)
                .amount(amount)
                .currency(currency)
                .bookingCommissionPct(bookingCommissionPct)
                .experienceItems(experienceItems)
                .deliverables(deliverables)
                .build();
    }

    public Long id() { return id; }
    public Long pitchId() { return pitchId; }
    public int roundNumber() { return roundNumber; }
    public OfferedBy offeredBy() { return offeredBy; }
    public String message() { return message; }
    public CompensationType compensationType() { return compensationType; }
    public Integer nights() { return nights; }
    public BigDecimal amount() { return amount; }
    public String currency() { return currency; }
    public BigDecimal bookingCommissionPct() { return bookingCommissionPct; }
    public List<String> experienceItems() { return experienceItems; }
    public List<CounterOfferDeliverable> deliverables() { return deliverables; }
    public OffsetDateTime createdAt() { return createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CounterOffer that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Long id;
        private Long pitchId;
        private int roundNumber;
        private OfferedBy offeredBy;
        private String message;
        private CompensationType compensationType;
        private Integer nights;
        private BigDecimal amount;
        private String currency;
        private BigDecimal bookingCommissionPct;
        private List<String> experienceItems;
        private List<CounterOfferDeliverable> deliverables;
        private OffsetDateTime createdAt;

        private Builder() {}

        public Builder id(Long id) { this.id = id; return this; }
        public Builder pitchId(Long pitchId) { this.pitchId = pitchId; return this; }
        public Builder roundNumber(int roundNumber) { this.roundNumber = roundNumber; return this; }
        public Builder offeredBy(OfferedBy offeredBy) { this.offeredBy = offeredBy; return this; }
        public Builder message(String message) { this.message = message; return this; }
        public Builder compensationType(CompensationType compensationType) { this.compensationType = compensationType; return this; }
        public Builder nights(Integer nights) { this.nights = nights; return this; }
        public Builder amount(BigDecimal amount) { this.amount = amount; return this; }
        public Builder currency(String currency) { this.currency = currency; return this; }
        public Builder bookingCommissionPct(BigDecimal bookingCommissionPct) { this.bookingCommissionPct = bookingCommissionPct; return this; }
        public Builder experienceItems(List<String> experienceItems) { this.experienceItems = experienceItems; return this; }
        public Builder deliverables(List<CounterOfferDeliverable> deliverables) { this.deliverables = deliverables; return this; }
        public Builder createdAt(OffsetDateTime createdAt) { this.createdAt = createdAt; return this; }

        public CounterOffer build() { return new CounterOffer(this); }
    }
}
