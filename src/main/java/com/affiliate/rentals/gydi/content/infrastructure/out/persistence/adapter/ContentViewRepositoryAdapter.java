package com.affiliate.rentals.gydi.content.infrastructure.out.persistence.adapter;

import com.affiliate.rentals.gydi.content.application.port.ContentViewRepositoryPort;
import com.affiliate.rentals.gydi.content.domain.model.ContentView;
import com.affiliate.rentals.gydi.content.infrastructure.out.persistence.entity.ContentViewEntity;
import com.affiliate.rentals.gydi.content.infrastructure.out.persistence.repository.ContentViewJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class ContentViewRepositoryAdapter implements ContentViewRepositoryPort {

    private final ContentViewJpaRepository jpaRepository;

    @Override
    public ContentView save(ContentView view) {
        ContentViewEntity entity = new ContentViewEntity(
                view.id(),
                view.contentPostId(),
                view.viewerId(),
                view.viewerIp(),
                view.watchDurationSeconds(),
                view.source(),
                view.createdAt()
        );
        ContentViewEntity saved = jpaRepository.save(entity);
        return ContentView.reconstitute(
                saved.getId(),
                saved.getContentPostId(),
                saved.getViewerId(),
                saved.getViewerIp(),
                saved.getWatchDurationSeconds(),
                saved.getSource(),
                saved.getCreatedAt()
        );
    }

    @Override
    public boolean existsByContentPostIdAndViewerIdAndDate(Long contentPostId, Long viewerId, LocalDate date) {
        return jpaRepository.existsByPostAndViewerAndDate(contentPostId, viewerId, date);
    }
}
