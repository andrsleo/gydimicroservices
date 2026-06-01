package com.affiliate.rentals.gydi.collaborations.application.port.out;

import com.affiliate.rentals.gydi.collaborations.domain.model.DeliveryAsset;

import java.util.List;
import java.util.Optional;

/**
 * Output port for DeliveryAsset persistence operations.
 */
public interface DeliveryAssetRepositoryPort {

    DeliveryAsset save(DeliveryAsset asset);

    Optional<DeliveryAsset> findById(Long id);

    List<DeliveryAsset> findByDeliverableId(Long agreementDeliverableId);
}
