package com.affiliate.rentals.gydi.content.application.usecase;

import com.affiliate.rentals.gydi.content.application.port.ContentPostRepositoryPort;
import com.affiliate.rentals.gydi.content.application.port.ContentViewRepositoryPort;
import com.affiliate.rentals.gydi.content.domain.model.ContentView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Use case for registering a content view event.
 *
 * <p>Deduplication logic: authenticated users are counted only once per post per day,
 * using a unique DB index on (content_post_id, viewer_id, date). Anonymous views
 * (no viewerId) are always recorded.</p>
 *
 * @author GYDI Development Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegisterContentViewUseCase {

    private final ContentViewRepositoryPort contentViewRepository;
    private final ContentPostRepositoryPort contentPostRepository;

    /**
     * Executes the use case.
     *
     * @param contentPostId the ID of the content post being viewed
     * @param viewerId      the ID of the authenticated viewer (null for anonymous)
     * @param viewerIp      the IP address of the viewer (may be null)
     */
    @Transactional
    public void execute(Long contentPostId, Long viewerId, String viewerIp) {
        // Dedup: same authenticated user + same post + same day
        if (viewerId != null) {
            if (contentViewRepository.existsByContentPostIdAndViewerIdAndDate(
                    contentPostId, viewerId, LocalDate.now())) {
                log.debug("[RegisterContentViewUseCase] Duplicate view skipped for postId={} viewerId={}", contentPostId, viewerId);
                return;
            }
        }
        ContentView view = ContentView.register(contentPostId, viewerId, viewerIp, "FEED");
        contentViewRepository.save(view);
        contentPostRepository.incrementViewCount(contentPostId);
        log.debug("[RegisterContentViewUseCase] View registered for postId={}", contentPostId);
    }
}
