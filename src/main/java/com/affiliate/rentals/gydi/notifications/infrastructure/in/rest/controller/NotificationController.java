package com.affiliate.rentals.gydi.notifications.infrastructure.in.rest.controller;

import com.affiliate.rentals.gydi.notifications.application.dto.NotificationDto;
import com.affiliate.rentals.gydi.notifications.application.dto.UnreadCountDto;
import com.affiliate.rentals.gydi.notifications.application.usecase.GetNotificationsUseCase;
import com.affiliate.rentals.gydi.notifications.application.usecase.GetUnreadCountUseCase;
import com.affiliate.rentals.gydi.notifications.application.usecase.MarkAllNotificationsReadUseCase;
import com.affiliate.rentals.gydi.notifications.application.usecase.MarkNotificationReadUseCase;
import com.affiliate.rentals.gydi.shared.security.OwnershipValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class NotificationController {

    private final GetNotificationsUseCase getNotificationsUseCase;
    private final GetUnreadCountUseCase getUnreadCountUseCase;
    private final MarkNotificationReadUseCase markNotificationReadUseCase;
    private final MarkAllNotificationsReadUseCase markAllNotificationsReadUseCase;
    private final OwnershipValidator ownershipValidator;

    /**
     * GET /api/v1/notifications?page=0&size=20
     * Returns paginated notifications for the authenticated user.
     */
    @GetMapping
    public ResponseEntity<Page<NotificationDto>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = ownershipValidator.getAuthenticatedUserId();
        return ResponseEntity.ok(getNotificationsUseCase.execute(userId, page, size));
    }

    /**
     * GET /api/v1/notifications/unread-count
     * Returns unread notification count for the authenticated user.
     */
    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountDto> getUnreadCount() {
        Long userId = ownershipValidator.getAuthenticatedUserId();
        return ResponseEntity.ok(getUnreadCountUseCase.execute(userId));
    }

    /**
     * PUT /api/v1/notifications/{id}/read
     * Marks a single notification as read. Only the recipient can mark their own.
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long id) {
        Long userId = ownershipValidator.getAuthenticatedUserId();
        markNotificationReadUseCase.execute(id, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * PUT /api/v1/notifications/read-all
     * Marks all notifications for the authenticated user as read.
     */
    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllRead() {
        Long userId = ownershipValidator.getAuthenticatedUserId();
        markAllNotificationsReadUseCase.execute(userId);
        return ResponseEntity.noContent().build();
    }
}
