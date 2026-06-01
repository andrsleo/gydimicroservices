package com.affiliate.rentals.gydi.collaborations.domain.model;

import com.affiliate.rentals.gydi.collaborations.domain.model.enums.CompensationType;
import com.affiliate.rentals.gydi.collaborations.domain.model.enums.OfferedBy;
import com.affiliate.rentals.gydi.collaborations.domain.model.enums.PitchStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("CreatorPitch — state machine")
class CreatorPitchTest {

    private CreatorPitch pitch;

    @BeforeEach
    void setUp() {
        pitch = CreatorPitch.create(
                1L, 10L, 20L,
                "Great property for my lifestyle content",
                "https://portfolio.example.com",
                LocalDate.now().plusDays(30),
                LocalDate.now().plusDays(37),
                List.of(),
                PitchCompensation.freeStay(7)
        );
    }

    // ── Factory ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("new pitch starts in PENDING status")
        void new_pitch_is_PENDING() {
            assertThat(pitch.status()).isEqualTo(PitchStatus.PENDING);
        }

        @Test
        @DisplayName("new pitch has 0 counter-offer rounds")
        void new_pitch_has_zero_rounds() {
            assertThat(pitch.counterOfferRounds()).isZero();
        }

        @Test
        @DisplayName("new pitch has expiresAt ~7 days from now")
        void new_pitch_expires_in_7_days() {
            assertThat(pitch.expiresAt()).isAfter(OffsetDateTime.now().plusDays(6));
            assertThat(pitch.expiresAt()).isBefore(OffsetDateTime.now().plusDays(8));
        }

        @Test
        @DisplayName("rejects introduction longer than 1000 chars")
        void rejects_long_introduction() {
            String tooLong = "x".repeat(1001);
            assertThatThrownBy(() -> CreatorPitch.create(
                    1L, 10L, 20L, tooLong, null,
                    LocalDate.now().plusDays(1), LocalDate.now().plusDays(8),
                    List.of(), PitchCompensation.freeStay(7)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("1000");
        }

        @Test
        @DisplayName("rejects checkOut not after checkIn")
        void rejects_invalid_dates() {
            LocalDate same = LocalDate.now().plusDays(5);
            assertThatThrownBy(() -> CreatorPitch.create(
                    1L, 10L, 20L, "intro", null, same, same,
                    List.of(), PitchCompensation.freeStay(7)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("checkOut must be after checkIn");
        }
    }

    // ── accept() ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("accept()")
    class Accept {

        @Test
        @DisplayName("PENDING → ACCEPTED")
        void pending_to_accepted() {
            pitch.accept();
            assertThat(pitch.status()).isEqualTo(PitchStatus.ACCEPTED);
        }

        @Test
        @DisplayName("COUNTERED → ACCEPTED")
        void countered_to_accepted() {
            pitch.addCounterOffer(counterOffer(OfferedBy.CREATOR));
            pitch.accept();
            assertThat(pitch.status()).isEqualTo(PitchStatus.ACCEPTED);
        }

        @Test
        @DisplayName("DECLINED pitch cannot be accepted")
        void declined_cannot_be_accepted() {
            pitch.decline(null);
            assertThatThrownBy(pitch::accept).isInstanceOf(IllegalStateException.class);
        }
    }

    // ── decline() ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("decline()")
    class Decline {

        @Test
        @DisplayName("PENDING → DECLINED, reason stored")
        void pending_to_declined_with_reason() {
            pitch.decline("Not the right fit");
            assertThat(pitch.status()).isEqualTo(PitchStatus.DECLINED);
            assertThat(pitch.declinedReason()).isEqualTo("Not the right fit");
        }

        @Test
        @DisplayName("DECLINED pitch cannot be declined again")
        void cannot_decline_twice() {
            pitch.decline(null);
            assertThatThrownBy(() -> pitch.decline(null)).isInstanceOf(IllegalStateException.class);
        }
    }

    // ── cancel() ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("cancel()")
    class Cancel {

        @Test
        @DisplayName("PENDING → CANCELLED")
        void pending_to_cancelled() {
            pitch.cancel();
            assertThat(pitch.status()).isEqualTo(PitchStatus.CANCELLED);
        }

        @Test
        @DisplayName("ACCEPTED pitch cannot be cancelled")
        void accepted_cannot_be_cancelled() {
            pitch.accept();
            assertThatThrownBy(pitch::cancel).isInstanceOf(IllegalStateException.class);
        }
    }

    // ── expire() ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("expire()")
    class Expire {

        @Test
        @DisplayName("PENDING → EXPIRED")
        void pending_to_expired() {
            pitch.expire();
            assertThat(pitch.status()).isEqualTo(PitchStatus.EXPIRED);
        }

        @Test
        @DisplayName("COUNTERED → EXPIRED")
        void countered_to_expired() {
            pitch.addCounterOffer(counterOffer(OfferedBy.CREATOR));
            pitch.expire();
            assertThat(pitch.status()).isEqualTo(PitchStatus.EXPIRED);
        }
    }

    // ── addCounterOffer() ────────────────────────────────────────────────────

    @Nested
    @DisplayName("addCounterOffer()")
    class AddCounterOffer {

        @Test
        @DisplayName("PENDING → COUNTERED, round count increments")
        void pending_to_countered() {
            pitch.addCounterOffer(counterOffer(OfferedBy.CREATOR));
            assertThat(pitch.status()).isEqualTo(PitchStatus.COUNTERED);
            assertThat(pitch.counterOfferRounds()).isEqualTo(1);
        }

        @Test
        @DisplayName("counter-offer resets expiresAt 7 days from now")
        void resets_expiration() {
            OffsetDateTime before = pitch.expiresAt();
            pitch.addCounterOffer(counterOffer(OfferedBy.CREATOR));
            assertThat(pitch.expiresAt()).isAfterOrEqualTo(before);
        }

        @Test
        @DisplayName("max 3 rounds — 4th throws IllegalStateException")
        void max_three_rounds() {
            pitch.addCounterOffer(counterOffer(OfferedBy.CREATOR));
            pitch.addCounterOffer(counterOffer(OfferedBy.HOST));
            pitch.addCounterOffer(counterOffer(OfferedBy.CREATOR));

            assertThatThrownBy(() -> pitch.addCounterOffer(counterOffer(OfferedBy.HOST)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Maximum negotiation rounds");
        }

        @Test
        @DisplayName("canCounterOffer() is false after 3 rounds")
        void canCounterOffer_false_after_max() {
            pitch.addCounterOffer(counterOffer(OfferedBy.CREATOR));
            pitch.addCounterOffer(counterOffer(OfferedBy.HOST));
            pitch.addCounterOffer(counterOffer(OfferedBy.CREATOR));

            assertThat(pitch.canCounterOffer()).isFalse();
        }
    }

    // ── nextResponder() ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("nextResponder()")
    class NextResponder {

        @Test
        @DisplayName("no counter-offers → HOST responds next")
        void no_offers_host_responds() {
            assertThat(pitch.nextResponder()).isEqualTo(OfferedBy.HOST);
        }

        @Test
        @DisplayName("after HOST counter-offer → CREATOR responds next")
        void after_host_offer_creator_responds() {
            pitch.addCounterOffer(counterOffer(OfferedBy.CREATOR)); // round 1: creator sent
            // nextResponder after creator sent = HOST
            assertThat(pitch.nextResponder()).isEqualTo(OfferedBy.HOST);
        }

        @Test
        @DisplayName("after CREATOR counter-offer → HOST responds next")
        void after_creator_offer_host_responds() {
            pitch.addCounterOffer(counterOffer(OfferedBy.CREATOR));
            pitch.addCounterOffer(counterOffer(OfferedBy.HOST));
            assertThat(pitch.nextResponder()).isEqualTo(OfferedBy.CREATOR);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private CounterOffer counterOffer(OfferedBy offeredBy) {
        int nextRound = pitch.counterOfferRounds() + 1;
        return CounterOffer.builder()
                .pitchId(1L)
                .roundNumber(Math.min(nextRound, 3))
                .offeredBy(offeredBy)
                .message("Let's negotiate")
                .compensationType(CompensationType.FREE_STAY)
                .createdAt(OffsetDateTime.now())
                .build();
    }
}
