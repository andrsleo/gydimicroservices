package com.affiliate.rentals.gydi.notifications.infrastructure.in.events;

import com.affiliate.rentals.gydi.notifications.application.usecase.CreateNotificationUseCase;
import com.affiliate.rentals.gydi.notifications.domain.model.NotificationType;
import com.affiliate.rentals.gydi.shared.events.ContentLikedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class ContentLikedEventListener {

    private static final Logger log = LoggerFactory.getLogger(ContentLikedEventListener.class);

    private final CreateNotificationUseCase createNotification;

    public ContentLikedEventListener(CreateNotificationUseCase createNotification) {
        this.createNotification = createNotification;
    }

    @Async
    @EventListener
    public void onContentLiked(ContentLikedEvent event) {
        // Don't notify if the creator liked their own post
        if (event.creatorUserId().equals(event.likedByUserId())) return;

        log.debug("Creating NEW_LIKE notification for creator {} on post {}",
                event.creatorUserId(), event.contentPostId());

        createNotification.execute(
                event.creatorUserId(),
                NotificationType.NEW_LIKE,
                "Alguien le dio like a tu contenido",
                null,
                event.contentPostId(),
                "CONTENT_POST"
        );
    }
}
