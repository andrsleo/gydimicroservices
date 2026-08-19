package com.affiliate.rentals.gydi.properties.domain.ports.in;

import java.util.List;
import com.affiliate.rentals.gydi.properties.domain.model.PropertyCalendarBlock;

public interface GetPropertyCalendarBlocksUseCase {
    List<PropertyCalendarBlock> getBlocks(Long propertyId);
}
