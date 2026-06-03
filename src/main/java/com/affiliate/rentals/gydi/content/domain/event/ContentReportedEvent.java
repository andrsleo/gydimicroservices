package com.affiliate.rentals.gydi.content.domain.event;

import com.affiliate.rentals.gydi.content.domain.model.ReportReason;

/**
 * Domain event published when a content post is reported by a user.
 *
 * @param contentPostId the ID of the reported content post
 * @param reporterId    the ID of the user who submitted the report
 * @param reason        the reason for the report
 * @author GYDI Development Team
 */
public record ContentReportedEvent(Long contentPostId, Long reporterId, ReportReason reason) {
}
