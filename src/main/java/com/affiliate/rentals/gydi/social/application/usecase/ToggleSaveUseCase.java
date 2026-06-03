package com.affiliate.rentals.gydi.social.application.usecase;

import com.affiliate.rentals.gydi.content.application.port.ContentPostRepositoryPort;
import com.affiliate.rentals.gydi.social.application.dto.ToggleSaveResponse;
import com.affiliate.rentals.gydi.social.application.port.SaveRepositoryPort;
import com.affiliate.rentals.gydi.social.domain.model.Save;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ToggleSaveUseCase {

    private final SaveRepositoryPort saveRepository;
    private final ContentPostRepositoryPort contentPostRepository;

    @Transactional
    public ToggleSaveResponse execute(Long userId, Long contentPostId) {
        var postOpt = contentPostRepository.findById(contentPostId);
        if (postOpt.isEmpty()) return new ToggleSaveResponse(false, 0);

        var post = postOpt.get();

        if (saveRepository.existsByUserIdAndContentPostId(userId, contentPostId)) {
            saveRepository.deleteByUserIdAndContentPostId(userId, contentPostId);
            post.decrementSaveCount();
            contentPostRepository.save(post);
            return new ToggleSaveResponse(false, post.saveCount());
        } else {
            saveRepository.save(Save.create(userId, contentPostId));
            post.incrementSaveCount();
            contentPostRepository.save(post);
            return new ToggleSaveResponse(true, post.saveCount());
        }
    }
}
