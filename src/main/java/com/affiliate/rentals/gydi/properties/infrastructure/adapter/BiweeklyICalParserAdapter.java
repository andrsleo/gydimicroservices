package com.affiliate.rentals.gydi.properties.infrastructure.adapter;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.affiliate.rentals.gydi.properties.domain.model.DateRange;

import biweekly.Biweekly;
import biweekly.ICalendar;
import biweekly.component.VEvent;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class BiweeklyICalParserAdapter {

    public List<DateRange> parse(InputStream iCalStream) {
        List<DateRange> blockedRanges = new ArrayList<>();
        try {
            ICalendar ical = Biweekly.parse(iCalStream).first();
            if (ical == null) {
                return blockedRanges;
            }

            for (VEvent event : ical.getEvents()) {
                java.util.Date start = event.getDateStart().getValue();
                java.util.Date end = event.getDateEnd().getValue();

                if (start != null && end != null) {
                    LocalDate startDate = start.toInstant().atZone(ZoneId.of("UTC")).toLocalDate();
                    LocalDate endDate = end.toInstant().atZone(ZoneId.of("UTC")).toLocalDate();

                    // iCal end dates are exclusive, so we subtract 1 day to get inclusive range
                    // Example: Event [2023-01-01, 2023-01-02) means only 2023-01-01 is blocked.
                    if (endDate.isAfter(startDate)) {
                        blockedRanges.add(new DateRange(startDate, endDate.minusDays(1)));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error parsing iCal stream", e);
        }
        return blockedRanges;
    }
}
