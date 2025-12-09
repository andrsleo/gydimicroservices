package com.affiliate.rentals.gydi.properties.domain.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.affiliate.rentals.gydi.properties.domain.model.PropertyCalendarBlock;

@Repository
public interface PropertyCalendarBlockRepository extends JpaRepository<PropertyCalendarBlock, Long> {

    List<PropertyCalendarBlock> findByPropertyIdAndEndDateAfter(Long propertyId, LocalDate date);

    @Modifying
    @Query("DELETE FROM PropertyCalendarBlock b WHERE b.propertyId = :propertyId AND b.source = :source")
    void deleteByPropertyIdAndSource(Long propertyId, String source);
}
