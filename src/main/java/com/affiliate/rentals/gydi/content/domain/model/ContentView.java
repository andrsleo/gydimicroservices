package com.affiliate.rentals.gydi.content.domain.model;

import java.time.LocalDateTime;

/**
 * Domain model representing a single view event on a content post.
 *
 * <p>Pure Java — no Spring, JPA, or Lombok annotations.</p>
 *
 * @author GYDI Development Team
 */
public record ContentView(
        Long id,
        Long contentPostId,
        Long viewerId,
        String viewerIp,
        Integer watchDurationSeconds,
        String source,
        LocalDateTime createdAt
) {

    /**
     * Factory method for registering a new view event.
     *
     * @param contentPostId the ID of the content post being viewed
     * @param viewerId      the ID of the authenticated viewer (may be null for anonymous)
     * @param viewerIp      the IP address of the viewer (may be null)
     * @param source        the source context (FEED, PROFILE, PROPERTY, DIRECT, SHARE)
     * @return a new ContentView with createdAt=now and watchDuration=null
     */
    public static ContentView register(Long contentPostId, Long viewerId, String viewerIp, String source) {
        return new ContentView(null, contentPostId, viewerId, viewerIp, null, source, LocalDateTime.now());
    }

    /**
     * Factory method for reconstituting a persisted ContentView from the database.
     */
    public static ContentView reconstitute(
            Long id,
            Long contentPostId,
            Long viewerId,
            String viewerIp,
            Integer watchDurationSeconds,
            String source,
            LocalDateTime createdAt) {
        return new ContentView(id, contentPostId, viewerId, viewerIp, watchDurationSeconds, source, createdAt);
    }
}
