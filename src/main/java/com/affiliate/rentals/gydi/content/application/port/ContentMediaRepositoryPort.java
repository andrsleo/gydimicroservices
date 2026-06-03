package com.affiliate.rentals.gydi.content.application.port;

import com.affiliate.rentals.gydi.content.domain.model.ContentMedia;

import java.util.List;
import java.util.Optional;

/**
 * Output port for ContentMedia persistence operations.
 *
 * @author GYDI Development Team
 */
public interface ContentMediaRepositoryPort {

    ContentMedia save(ContentMedia media);

    Optional<ContentMedia> findById(Long id);

    List<ContentMedia> findByContentPostId(Long contentPostId);

    void deleteById(Long id);

    long countByContentPostId(Long contentPostId);
}
