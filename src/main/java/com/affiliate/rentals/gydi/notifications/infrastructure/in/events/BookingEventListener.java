package com.affiliate.rentals.gydi.notifications.infrastructure.in.events;

import com.affiliate.rentals.gydi.notifications.application.usecase.CreateNotificationUseCase;
import com.affiliate.rentals.gydi.notifications.domain.model.NotificationType;
import com.affiliate.rentals.gydi.shared.events.BookingFromContentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class BookingEventListener {

    private static final Logger log = LoggerFactory.getLogger(BookingEventListener.class);

    private final CreateNotificationUseCase createNotification;

    public BookingEventListener(CreateNotificationUseCase createNotification) {
        this.createNotification = createNotification;
    }

    @Async
    @EventListener
    public void onBookingFromContent(BookingFromContentEvent event) {
        log.debug("Creating BOOKING_FROM_CONTENT notification for creator {} — booking {}",
                event.creatorUserId(), event.bookingId());

        createNotification.execute(
                event.creatorUserId(),
                NotificationType.BOOKING_FROM_CONTENT,
                "Tu contenido generó una reserva",
                "Un usuario reservó una propiedad a través de tu contenido.",
                event.bookingId(),
                "BOOKING"
        );
    }
}
