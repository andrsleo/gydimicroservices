package com.affiliate.rentals.gydi.notifications.infrastructure.in.events;

import com.affiliate.rentals.gydi.notifications.application.usecase.CreateNotificationUseCase;
import com.affiliate.rentals.gydi.notifications.domain.model.NotificationType;
import com.affiliate.rentals.gydi.shared.events.ContentMilestoneEvent;
import com.affiliate.rentals.gydi.shared.events.NewFollowerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class SocialEventListener {

    private static final Logger log = LoggerFactory.getLogger(SocialEventListener.class);

    private final CreateNotificationUseCase createNotification;

    public SocialEventListener(CreateNotificationUseCase createNotification) {
        this.createNotification = createNotification;
    }

    @Async
    @EventListener
    public void onNewFollower(NewFollowerEvent event) {
        log.debug("Creating NEW_FOLLOWER notification for user {}", event.followedUserId());

        createNotification.execute(
                event.followedUserId(),
                NotificationType.NEW_FOLLOWER,
                "Tienes un nuevo seguidor",
                null,
                event.followerUserId(),
                "FOLLOW"
        );
    }

    @Async
    @EventListener
    public void onContentMilestone(ContentMilestoneEvent event) {
        log.debug("Creating CONTENT_MILESTONE notification for creator {} — {}K views",
                event.creatorUserId(), event.milestoneViews() / 1000);

        String views = formatMilestone(event.milestoneViews());
        createNotification.execute(
                event.creatorUserId(),
                NotificationType.CONTENT_MILESTONE,
                "Tu contenido llegó a " + views + " vistas",
                null,
                event.contentPostId(),
                "CONTENT_POST"
        );
    }

    private String formatMilestone(long views) {
        if (views >= 1_000_000) return (views / 1_000_000) + "M";
        if (views >= 1_000) return (views / 1_000) + "K";
        return String.valueOf(views);
    }
}
