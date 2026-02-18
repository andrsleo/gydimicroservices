package com.affiliate.rentals.gydi.referrals.infrastructure.out.persistence;

import com.affiliate.rentals.gydi.referrals.domain.model.ReferralLink;
import com.affiliate.rentals.gydi.referrals.domain.model.ReferralLinkStatus;
import com.affiliate.rentals.gydi.referrals.domain.port.ReferralLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adapter que implementa ReferralLinkRepository usando JPA
 */
@Component
@RequiredArgsConstructor
public class ReferralLinkRepositoryAdapter implements ReferralLinkRepository {

    private final ReferralLinkJpaRepository jpaRepository;

    @Override
    public ReferralLink save(ReferralLink referralLink) {
        ReferralLinkJpaEntity entity = toEntity(referralLink);
        ReferralLinkJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public ReferralLink update(ReferralLink referralLink) {
        return save(referralLink); // JPA save hace upsert
    }

    @Override
    public Optional<ReferralLink> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<ReferralLink> findByEncryptedToken(String encryptedToken) {
        return jpaRepository.findByEncryptedToken(encryptedToken).map(this::toDomain);
    }

    @Override
    public Optional<ReferralLink> findByShortCode(String shortCode) {
        return jpaRepository.findByShortCode(shortCode).map(this::toDomain);
    }

    @Override
    public List<ReferralLink> findByAffiliateId(Long affiliateId) {
        return jpaRepository.findByAffiliateId(affiliateId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReferralLink> findByAffiliateIdAndStatus(Long affiliateId, ReferralLinkStatus status) {
        return jpaRepository.findByAffiliateIdAndStatus(affiliateId, status).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReferralLink> findByPropertyId(Long propertyId) {
        return jpaRepository.findByPropertyId(propertyId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReferralLink> findActiveLinksExpiringSoon(LocalDateTime fromDate, LocalDateTime toDate) {
        return jpaRepository.findActiveLinksExpiringSoon(fromDate, toDate).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReferralLink> findExpiredLinks() {
        return jpaRepository.findExpiredLinks().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsActiveLink(Long affiliateId, Long propertyId) {
        return jpaRepository.existsActiveLink(affiliateId, propertyId);
    }

    @Override
    public Optional<ReferralLink> findActiveLink(Long affiliateId, Long propertyId) {
        return jpaRepository.findActiveLink(affiliateId, propertyId).map(this::toDomain);
    }

    @Override
    public long countByAffiliateId(Long affiliateId) {
        return jpaRepository.countByAffiliateId(affiliateId);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public List<ReferralLink> findActiveExpired() {
        return jpaRepository.findActiveExpired().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    // Mappers
    private ReferralLinkJpaEntity toEntity(ReferralLink domain) {
        ReferralLinkJpaEntity entity = new ReferralLinkJpaEntity();
        entity.setId(domain.getId());
        entity.setAffiliateId(domain.getAffiliateId());
        entity.setPropertyId(domain.getPropertyId());
        entity.setEncryptedToken(domain.getEncryptedToken());
        entity.setShortCode(domain.getShortCode());
        entity.setClicksCount(domain.getClicksCount());
        entity.setStatus(domain.getStatus());
        entity.setExpiresAt(domain.getExpiresAt());
        entity.setDeletedAt(domain.getDeletedAt());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }

    private ReferralLink toDomain(ReferralLinkJpaEntity entity) {
        ReferralLink domain = ReferralLink.create(
                entity.getAffiliateId(),
                entity.getPropertyId(),
                entity.getEncryptedToken(),
                entity.getShortCode(),
                entity.getExpiresAt());
        domain.setId(entity.getId());
        domain.setClicksCount(entity.getClicksCount());
        domain.setStatus(entity.getStatus());
        domain.setDeletedAt(entity.getDeletedAt());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setUpdatedAt(entity.getUpdatedAt());
        return domain;
    }
}