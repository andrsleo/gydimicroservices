package com.affiliate.rentals.gydi.referrals.infrastructure.out.persistence;

import com.affiliate.rentals.gydi.referrals.domain.model.DeviceType;
import com.affiliate.rentals.gydi.referrals.domain.model.ReferralClick;
import com.affiliate.rentals.gydi.referrals.domain.port.ReferralClickRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adapter que implementa ReferralClickRepository usando JPA
 */
@Component
@RequiredArgsConstructor
public class ReferralClickRepositoryAdapter implements ReferralClickRepository {

    private final ReferralClickJpaRepository jpaRepository;

    @Override
    public ReferralClick save(ReferralClick click) {
        ReferralClickJpaEntity entity = toEntity(click);
        ReferralClickJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<ReferralClick> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<ReferralClick> findByReferralLinkId(Long referralLinkId) {
        return jpaRepository.findByReferralLinkId(referralLinkId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<ReferralClick> findByReferralLinkIdAndDateRange(Long referralLinkId,
                                                                  LocalDateTime from,
                                                                  LocalDateTime to) {
        return jpaRepository.findByReferralLinkIdAndDateRange(referralLinkId, from, to).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public long countByReferralLinkId(Long referralLinkId) {
        return jpaRepository.countByReferralLinkId(referralLinkId);
    }

    @Override
    public long countByReferralLinkIdAndDateRange(Long referralLinkId,
                                                   LocalDateTime from,
                                                   LocalDateTime to) {
        return jpaRepository.countByReferralLinkIdAndDateRange(referralLinkId, from, to);
    }

    @Override
    public List<ReferralClick> findProbableBots(int botScoreThreshold,
                                                 LocalDateTime from,
                                                 LocalDateTime to) {
        return jpaRepository.findProbableBots(botScoreThreshold, from, to).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public Map<DeviceType, Long> countByDeviceType(Long referralLinkId) {
        List<ReferralClickJpaEntity> clicks = jpaRepository.findByReferralLinkId(referralLinkId);

        Map<DeviceType, Long> deviceTypeCounts = new HashMap<>();
        for (ReferralClickJpaEntity click : clicks) {
            DeviceType deviceType = click.getDeviceType();
            deviceTypeCounts.put(deviceType, deviceTypeCounts.getOrDefault(deviceType, 0L) + 1);
        }

        return deviceTypeCounts;
    }

    @Override
    public Map<String, Long> countByCountry(Long referralLinkId) {
        List<ReferralClickJpaEntity> clicks = jpaRepository.findByReferralLinkId(referralLinkId);

        Map<String, Long> countryCounts = new HashMap<>();
        for (ReferralClickJpaEntity click : clicks) {
            String countryCode = click.getCountryCode();
            if (countryCode != null) {
                countryCounts.put(countryCode, countryCounts.getOrDefault(countryCode, 0L) + 1);
            }
        }

        return countryCounts;
    }

    @Override
    public List<ReferralClick> findDuplicateClicks(Long referralLinkId,
                                                    byte[] ipHash,
                                                    byte[] userAgentHash,
                                                    LocalDateTime since) {
        return jpaRepository.findDuplicateClicks(referralLinkId, ipHash, userAgentHash, since).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public int deleteClicksOlderThan(LocalDateTime beforeDate) {
        return jpaRepository.deleteClicksOlderThan(beforeDate);
    }

    // Mappers
    private ReferralClickJpaEntity toEntity(ReferralClick domain) {
        ReferralClickJpaEntity entity = new ReferralClickJpaEntity();
        entity.setId(domain.getId());
        entity.setReferralLinkId(domain.getReferralLinkId());
        entity.setIpAddressHash(domain.getIpAddressHash());
        entity.setUserAgentHash(domain.getUserAgentHash());
        entity.setFingerprint(domain.getFingerprint());
        entity.setCountryCode(domain.getCountryCode());
        entity.setDeviceType(domain.getDeviceType());
        entity.setBotScore(domain.getBotScore());
        entity.setClickedAt(domain.getClickedAt());
        return entity;
    }

    private ReferralClick toDomain(ReferralClickJpaEntity entity) {
        ReferralClick domain = ReferralClick.create(
            entity.getReferralLinkId(),
            entity.getIpAddressHash(),
            entity.getUserAgentHash(),
            entity.getFingerprint(),
            entity.getCountryCode(),
            entity.getDeviceType(),
            entity.getBotScore()
        );
        domain.setId(entity.getId());
        domain.setClickedAt(entity.getClickedAt());
        return domain;
    }
}