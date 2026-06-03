package com.affiliate.rentals.gydi.social.application.dto;

import java.time.LocalDateTime;

public record FollowUserDto(Long userId, LocalDateTime followedAt) {}
