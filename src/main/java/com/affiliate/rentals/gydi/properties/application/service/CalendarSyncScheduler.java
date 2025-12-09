package com.affiliate.rentals.gydi.properties.application.service;

import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.affiliate.rentals.gydi.properties.domain.model.DateRange;
import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.model.PropertyCalendarBlock;
import com.affiliate.rentals.gydi.properties.domain.repository.PropertyCalendarBlockRepository;
import com.affiliate.rentals.gydi.properties.domain.ports.out.PropertyRepositoryPort;
import com.affiliate.rentals.gydi.properties.infrastructure.adapter.BiweeklyICalParserAdapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

@Service
@RequiredArgsConstructor
@Slf4j
public class CalendarSyncScheduler {

    private final PropertyRepositoryPort propertyRepository;
    private final PropertyCalendarBlockRepository blockRepository;
    private final BiweeklyICalParserAdapter icalParser;

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.HOURS)
    @SchedulerLock(name = "CalendarSyncScheduler_syncAirbnbCalendars", lockAtLeastFor = "5m", lockAtMostFor = "15m")
    @Transactional
    public void syncAirbnbCalendars() {
        log.info("Starting Airbnb calendar sync job...");

        List<Property> properties = propertyRepository.findAllWithIcalUrl();

        for (Property property : properties) {
            syncProperty(property);
        }

        log.info("Airbnb calendar sync job completed. Processed {} properties.", properties.size());
    }

    private void syncProperty(Property property) {
        try {
            log.debug("Syncing calendar for property: {}", property.getId());

            // 1. Fetch iCal content
            try (InputStream is = URI.create(property.getIcalUrlAirbnb()).toURL().openStream()) {

                // 2. Parse
                List<DateRange> blockedRanges = icalParser.parse(is);

                // 3. Update DB
                // Delete existing AIRBNB blocks for this property
                blockRepository.deleteByPropertyIdAndSource(property.getId().getValue(), "AIRBNB");

                // Insert new blocks
                List<PropertyCalendarBlock> blocks = blockedRanges.stream()
                        .map(range -> PropertyCalendarBlock.builder()
                                .propertyId(property.getId().getValue())
                                .startDate(range.start())
                                .endDate(range.end())
                                .source("AIRBNB")
                                .build())
                        .toList();

                blockRepository.saveAll(blocks);

                log.info("Synced {} blocks for property {}", blocks.size(), property.getId());
            }
        } catch (Exception e) {
            log.error("Failed to sync calendar for property {}", property.getId(), e);
        }
    }
}
