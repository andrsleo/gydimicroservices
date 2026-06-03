package com.affiliate.rentals.gydi.content.application.dto;

import java.util.List;

/**
 * Cursor-based paginated response DTO for a content feed.
 *
 * @param content    the list of content posts in this page
 * @param nextCursor opaque cursor pointing to the next page (null if no more pages)
 * @param hasMore    whether there are more pages available
 * @author GYDI Development Team
 */
public record ContentFeedPageDto(
        List<ContentPostDto> content,
        String nextCursor,
        boolean hasMore
) {
}
