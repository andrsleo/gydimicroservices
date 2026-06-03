package com.affiliate.rentals.gydi.content.application.dto;

/**
 * A single creator entry from the {@code content.creator_leaderboard} materialized view.
 */
public record CreatorLeaderboardDto(
        Long creatorId,
        String displayName,
        String username,
        Integer contentCount,
        Long totalViews,
        Double avgEngagement,
        Long followerCount
) {}
