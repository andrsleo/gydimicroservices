package com.affiliate.rentals.gydi.content.infrastructure.out.persistence.adapter;

import com.affiliate.rentals.gydi.content.application.port.ContentMediaRepositoryPort;
import com.affiliate.rentals.gydi.content.domain.model.ContentMedia;
import com.affiliate.rentals.gydi.content.infrastructure.out.persistence.mapper.ContentMediaEntityMapper;
import com.affiliate.rentals.gydi.content.infrastructure.out.persistence.repository.ContentMediaJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ContentMediaRepositoryAdapter implements ContentMediaRepositoryPort {

    private final ContentMediaJpaRepository jpaRepository;
    private final ContentMediaEntityMapper mapper;

    @Override
    public ContentMedia save(ContentMedia media) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(media)));
    }

    @Override
    public Optional<ContentMedia> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<ContentMedia> findByContentPostId(Long contentPostId) {
        return jpaRepository.findByContentPostIdOrderByDisplayOrderAsc(contentPostId)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public long countByContentPostId(Long contentPostId) {
        return jpaRepository.countByContentPostId(contentPostId);
    }
}
