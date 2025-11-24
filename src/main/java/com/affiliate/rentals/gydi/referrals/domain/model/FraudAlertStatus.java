package com.affiliate.rentals.gydi.referrals.domain.model;

/**
 * Estado de una alerta de fraude
 */
public enum FraudAlertStatus {
    /**
     * Alerta pendiente de revisión
     */
    PENDING,

    /**
     * Alerta en investigación por el equipo de seguridad
     */
    UNDER_INVESTIGATION,

    /**
     * Fraude confirmado
     */
    CONFIRMED,

    /**
     * Falso positivo - No es fraude
     */
    FALSE_POSITIVE,

    /**
     * Alerta resuelta (acción tomada)
     */
    RESOLVED
}