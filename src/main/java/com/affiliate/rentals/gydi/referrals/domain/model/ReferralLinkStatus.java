package com.affiliate.rentals.gydi.referrals.domain.model;

/**
 * Estado del enlace de referido
 */
public enum ReferralLinkStatus {
    /**
     * Enlace activo y funcional
     */
    ACTIVE,

    /**
     * Enlace desactivado temporalmente por el usuario
     */
    PAUSED,

    /**
     * Enlace expirado por fecha
     */
    EXPIRED,

    /**
     * Enlace eliminado (soft delete)
     */
    DISABLED
}