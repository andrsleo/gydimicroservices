package com.affiliate.rentals.gydi.calendar.application.dto;

import com.affiliate.rentals.gydi.properties.domain.model.Money;

public record MoneyDto(Double amount, String currency) {

    public static MoneyDto of(Money money) {
        return new MoneyDto(money.amount().doubleValue(), money.currency().getCurrencyCode());
    }
}
