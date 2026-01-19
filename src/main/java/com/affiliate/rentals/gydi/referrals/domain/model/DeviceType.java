package com.affiliate.rentals.gydi.referrals.domain.model;

/**
 * Tipo de dispositivo desde el cual se hizo click en el enlace
 */
public enum DeviceType {
    /**
     * Computadora de escritorio
     */
    DESKTOP,

    /**
     * Dispositivo móvil (teléfono)
     */
    MOBILE,

    /**
     * Tableta
     */
    TABLET,

    /**
     * Tráfico automatizado (Bots/Crawlers)
     */
    BOT,

    /**
     * Dispositivo desconocido o no clasificable
     */
    UNKNOWN
}