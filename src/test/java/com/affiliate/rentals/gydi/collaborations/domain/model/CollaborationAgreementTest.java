package com.affiliate.rentals.gydi.collaborations.domain.model;

import com.affiliate.rentals.gydi.collaborations.domain.model.enums.AgreementStatus;
import com.affiliate.rentals.gydi.collaborations.domain.model.enums.DeliverableType;
import com.affiliate.rentals.gydi.collaborations.domain.model.enums.OfferedBy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("CollaborationAgreement")
class CollaborationAgreementTest {

    private static final Long HOST_ID    = 10L;
    private static final Long CREATOR_ID = 20L;

    // ── Factory ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("fromPitch")
    class FromPitch {

        @Test
        @DisplayName("creates ACTIVE agreement with computed deadlines")
        void creates_active_agreement_with_deadlines() {
            LocalDate checkIn  = LocalDate.now().plusDays(30);
            LocalDate checkOut = LocalDate.now().plusDays(37);

            CollaborationAgreement agreement = CollaborationAgreement.fromPitch(
                    1L, 1L, HOST_ID, CREATOR_ID, checkIn, checkOut,
                    List.of(), List.of(), PitchCompensation.freeStay(7));

            assertThat(agreement.status()).isEqualTo(AgreementStatus.ACTIVE);
            assertThat(agreement.deliveryDeadline()).isEqualTo(checkOut.plusDays(14));
            assertThat(agreement.postingDeadline()).isEqualTo(checkOut.plusDays(14 + 7));
        }

        @Test
        @DisplayName("throws NullPointerException when required field missing")
        void throws_when_required_field_missing() {
            assertThatThrownBy(() -> CollaborationAgreement.fromPitch(
                    1L, 1L, null, CREATOR_ID,
                    LocalDate.now(), LocalDate.now().plusDays(7),
                    List.of(), List.of(), PitchCompensation.freeStay(7)))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    // ── State machine: startProgress ─────────────────────────────────────────

    @Nested
    @DisplayName("startProgress")
    class StartProgress {

        @Test
        @DisplayName("ACTIVE → IN_PROGRESS")
        void active_becomes_in_progress() {
            CollaborationAgreement agreement = activeAgreement(List.of());
            agreement.startProgress();
            assertThat(agreement.status()).isEqualTo(AgreementStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("throws from non-ACTIVE state")
        void throws_from_non_active() {
            CollaborationAgreement agreement = agreementWithStatus(AgreementStatus.CANCELLED);
            assertThatThrownBy(agreement::startProgress)
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // ── State machine: markDelivered ──────────────────────────────────────────

    @Nested
    @DisplayName("markDelivered")
    class MarkDelivered {

        @Test
        @DisplayName("IN_PROGRESS → DELIVERED")
        void in_progress_becomes_delivered() {
            CollaborationAgreement agreement = agreementWithStatus(AgreementStatus.IN_PROGRESS);
            agreement.markDelivered();
            assertThat(agreement.status()).isEqualTo(AgreementStatus.DELIVERED);
        }

        @Test
        @DisplayName("throws from ACTIVE (must be IN_PROGRESS)")
        void throws_from_active() {
            CollaborationAgreement agreement = activeAgreement(List.of());
            assertThatThrownBy(agreement::markDelivered)
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // ── State machine: complete ───────────────────────────────────────────────

    @Nested
    @DisplayName("complete")
    class Complete {

        @Test
        @DisplayName("DELIVERED → COMPLETED when all deliverables approved")
        void delivered_with_all_approved_completes() {
            AgreementDeliverable approved = approvedDeliverable(1L);
            CollaborationAgreement agreement = agreementWithStatus(AgreementStatus.DELIVERED,
                    List.of(approved));

            agreement.complete();

            assertThat(agreement.status()).isEqualTo(AgreementStatus.COMPLETED);
        }

        @Test
        @DisplayName("throws when not all deliverables approved")
        void throws_when_not_all_approved() {
            AgreementDeliverable submitted = submittedDeliverable(1L);
            CollaborationAgreement agreement = agreementWithStatus(AgreementStatus.DELIVERED,
                    List.of(submitted));

            assertThatThrownBy(agreement::complete)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not all deliverables are approved");
        }

        @Test
        @DisplayName("throws from ACTIVE (must be DELIVERED)")
        void throws_from_active() {
            CollaborationAgreement agreement = activeAgreement(List.of());
            assertThatThrownBy(agreement::complete)
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // ── State machine: cancel ─────────────────────────────────────────────────

    @Nested
    @DisplayName("cancel")
    class Cancel {

        @Test
        @DisplayName("ACTIVE → CANCELLED by HOST, records cancelledBy and cancelledAt")
        void host_cancels_active() {
            CollaborationAgreement agreement = activeAgreement(List.of());
            agreement.cancel(OfferedBy.HOST);

            assertThat(agreement.status()).isEqualTo(AgreementStatus.CANCELLED);
            assertThat(agreement.cancelledBy()).isEqualTo(OfferedBy.HOST);
            assertThat(agreement.cancelledAt()).isNotNull();
        }

        @Test
        @DisplayName("ACTIVE → CANCELLED by CREATOR")
        void creator_cancels_active() {
            CollaborationAgreement agreement = activeAgreement(List.of());
            agreement.cancel(OfferedBy.CREATOR);

            assertThat(agreement.status()).isEqualTo(AgreementStatus.CANCELLED);
            assertThat(agreement.cancelledBy()).isEqualTo(OfferedBy.CREATOR);
        }

        @Test
        @DisplayName("throws from COMPLETED (only ACTIVE can be cancelled)")
        void throws_from_completed() {
            CollaborationAgreement agreement = agreementWithStatus(AgreementStatus.COMPLETED);
            assertThatThrownBy(() -> agreement.cancel(OfferedBy.HOST))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // ── canCancel ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("canCancel")
    class CanCancel {

        @Test
        @DisplayName("true when ACTIVE")
        void true_when_active() {
            assertThat(activeAgreement(List.of()).canCancel()).isTrue();
        }

        @Test
        @DisplayName("false when COMPLETED")
        void false_when_completed() {
            assertThat(agreementWithStatus(AgreementStatus.COMPLETED).canCancel()).isFalse();
        }

        @Test
        @DisplayName("false when CANCELLED")
        void false_when_cancelled() {
            assertThat(agreementWithStatus(AgreementStatus.CANCELLED).canCancel()).isFalse();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private CollaborationAgreement activeAgreement(List<AgreementDeliverable> deliverables) {
        return CollaborationAgreement.builder()
                .id(50L)
                .pitchId(1L)
                .propertyId(1L)
                .hostId(HOST_ID)
                .creatorId(CREATOR_ID)
                .status(AgreementStatus.ACTIVE)
                .checkInDate(LocalDate.now().plusDays(30))
                .checkOutDate(LocalDate.now().plusDays(37))
                .deliverables(deliverables)
                .contentRights(List.of())
                .compensation(PitchCompensation.freeStay(7))
                .build();
    }

    private CollaborationAgreement agreementWithStatus(AgreementStatus status) {
        return agreementWithStatus(status, List.of());
    }

    private CollaborationAgreement agreementWithStatus(AgreementStatus status,
                                                        List<AgreementDeliverable> deliverables) {
        return CollaborationAgreement.builder()
                .id(50L)
                .pitchId(1L)
                .propertyId(1L)
                .hostId(HOST_ID)
                .creatorId(CREATOR_ID)
                .status(status)
                .checkInDate(LocalDate.now().plusDays(30))
                .checkOutDate(LocalDate.now().plusDays(37))
                .deliverables(deliverables)
                .contentRights(List.of())
                .compensation(PitchCompensation.freeStay(7))
                .build();
    }

    private AgreementDeliverable approvedDeliverable(Long id) {
        AgreementDeliverable d = AgreementDeliverable.reconstitute(
                id, 50L, DeliverableType.REEL, 1, "SUBMITTED", null, List.of());
        d.approve();
        return d;
    }

    private AgreementDeliverable submittedDeliverable(Long id) {
        return AgreementDeliverable.reconstitute(
                id, 50L, DeliverableType.REEL, 1, "SUBMITTED", null, List.of());
    }
}
