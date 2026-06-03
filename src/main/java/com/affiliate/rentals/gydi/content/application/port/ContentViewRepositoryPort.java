package com.affiliate.rentals.gydi.content.application.port;

import com.affiliate.rentals.gydi.content.domain.model.ContentView;

import java.time.LocalDate;

/**
 * Output port for ContentView persistence operations.
 *
 * @author GYDI Development Team
 */
public interface ContentViewRepositoryPort {

    ContentView save(ContentView view);

    boolean existsByContentPostIdAndViewerIdAndDate(Long contentPostId, Long viewerId, LocalDate date);
}
