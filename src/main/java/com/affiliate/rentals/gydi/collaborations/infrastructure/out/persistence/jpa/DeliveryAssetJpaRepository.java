package com.affiliate.rentals.gydi.collaborations.infrastructure.out.persistence.jpa;

import com.affiliate.rentals.gydi.collaborations.infrastructure.out.persistence.entity.DeliveryAssetEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliveryAssetJpaRepository extends JpaRepository<DeliveryAssetEntity, Long> {

    List<DeliveryAssetEntity> findByAgreementDeliverableId(Long agreementDeliverableId);
}
