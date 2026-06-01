package com.affiliate.rentals.gydi.notifications.infrastructure.in.events;

import com.affiliate.rentals.gydi.collaborations.domain.event.AgreementCancelledEvent;
import com.affiliate.rentals.gydi.collaborations.domain.event.CounterOfferReceivedEvent;
import com.affiliate.rentals.gydi.collaborations.domain.event.DeliveryApprovedEvent;
import com.affiliate.rentals.gydi.collaborations.domain.event.DeliverySubmittedEvent;
import com.affiliate.rentals.gydi.collaborations.domain.event.PitchAcceptedEvent;
import com.affiliate.rentals.gydi.collaborations.domain.event.PitchDeclinedEvent;
import com.affiliate.rentals.gydi.collaborations.domain.event.PitchExpiredEvent;
import com.affiliate.rentals.gydi.collaborations.domain.event.PitchReceivedEvent;
import com.affiliate.rentals.gydi.collaborations.domain.event.RevisionRequestedEvent;
import com.affiliate.rentals.gydi.notifications.application.usecase.CreateNotificationUseCase;
import com.affiliate.rentals.gydi.notifications.domain.model.NotificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listens to collaboration domain events and creates notifications for affected parties.
 *
 * <p>All listeners are asynchronous to avoid blocking the publishing transaction.
 * Uses REQUIRES_NEW propagation in {@link CreateNotificationUseCase} for isolation.
 */
@Component
public class CollaborationEventListener {

    private static final Logger log = LoggerFactory.getLogger(CollaborationEventListener.class);

    private final CreateNotificationUseCase createNotification;

    public CollaborationEventListener(CreateNotificationUseCase createNotification) {
        this.createNotification = createNotification;
    }

    @Async
    @EventListener
    public void onPitchReceived(PitchReceivedEvent event) {
        log.debug("Creating PITCH_RECEIVED notification for host {}", event.hostId());
        createNotification.execute(
                event.hostId(),
                NotificationType.PITCH_RECEIVED,
                "Recibiste una propuesta de colaboración",
                null,
                event.pitchId(),
                "PITCH"
        );
    }

    @Async
    @EventListener
    public void onPitchAccepted(PitchAcceptedEvent event) {
        log.debug("Creating PITCH_ACCEPTED notification for creator {}", event.creatorId());
        createNotification.execute(
                event.creatorId(),
                NotificationType.PITCH_ACCEPTED,
                "Tu propuesta fue aceptada",
                null,
                event.pitchId(),
                "PITCH"
        );
    }

    @Async
    @EventListener
    public void onPitchDeclined(PitchDeclinedEvent event) {
        log.debug("Creating PITCH_DECLINED notification for creator {}", event.creatorId());
        createNotification.execute(
                event.creatorId(),
                NotificationType.PITCH_DECLINED,
                "Tu propuesta no fue aceptada",
                null,
                event.pitchId(),
                "PITCH"
        );
    }

    @Async
    @EventListener
    public void onCounterOfferReceived(CounterOfferReceivedEvent event) {
        Long recipientId = event.recipientId();
        log.debug("Creating COUNTER_OFFER_RECEIVED notification for user {}", recipientId);
        createNotification.execute(
                recipientId,
                NotificationType.COUNTER_OFFER_RECEIVED,
                "Recibiste una contraoferta",
                null,
                event.pitchId(),
                "PITCH"
        );
    }

    @Async
    @EventListener
    public void onDeliverySubmitted(DeliverySubmittedEvent event) {
        log.debug("Creating DELIVERY_SUBMITTED notification for host {}", event.hostId());
        createNotification.execute(
                event.hostId(),
                NotificationType.DELIVERY_SUBMITTED,
                "El creador subió contenido para revisión",
                null,
                event.agreementId(),
                "AGREEMENT"
        );
    }

    @Async
    @EventListener
    public void onDeliveryApproved(DeliveryApprovedEvent event) {
        log.debug("Creating DELIVERY_APPROVED notification for creator {}", event.creatorId());
        String body = event.allApproved() ? "¡Todo tu contenido fue aprobado!" : null;
        createNotification.execute(
                event.creatorId(),
                NotificationType.DELIVERY_APPROVED,
                "Tu entrega fue aprobada",
                body,
                event.agreementId(),
                "AGREEMENT"
        );
    }

    @Async
    @EventListener
    public void onRevisionRequested(RevisionRequestedEvent event) {
        log.debug("Creating REVISION_REQUESTED notification for creator {}", event.creatorId());
        createNotification.execute(
                event.creatorId(),
                NotificationType.REVISION_REQUESTED,
                "El host solicitó una revisión",
                event.feedback(),
                event.agreementId(),
                "AGREEMENT"
        );
    }

    @Async
    @EventListener
    public void onPitchExpired(PitchExpiredEvent event) {
        log.debug("Creating PITCH_EXPIRED notification for creator {}", event.creatorId());
        createNotification.execute(
                event.creatorId(),
                NotificationType.PITCH_EXPIRED,
                "Tu propuesta expiró sin respuesta",
                null,
                event.pitchId(),
                "PITCH"
        );
    }

    @Async
    @EventListener
    public void onAgreementCancelled(AgreementCancelledEvent event) {
        // Notify the OTHER party (not the one who cancelled)
        Long notifyUserId = "HOST".equals(event.cancelledBy()) ? event.creatorId() : event.hostId();
        log.debug("Creating AGREEMENT_CANCELLED notification for user {}", notifyUserId);
        createNotification.execute(
                notifyUserId,
                NotificationType.AGREEMENT_CANCELLED,
                "El acuerdo de colaboración fue cancelado",
                null,
                event.agreementId(),
                "AGREEMENT"
        );
    }
}
