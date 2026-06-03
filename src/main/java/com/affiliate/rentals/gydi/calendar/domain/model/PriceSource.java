package com.affiliate.rentals.gydi.calendar.domain.model;

public enum PriceSource {
    BASE,
    SEASON_HIGH,
    SEASON_MEDIUM,
    SEASON_LOW,
    CUSTOM;

    public static PriceSource fromSeasonType(SeasonType seasonType) {
        return switch (seasonType) {
            case HIGH -> SEASON_HIGH;
            case MEDIUM -> SEASON_MEDIUM;
            case LOW -> SEASON_LOW;
        };
    }
}
