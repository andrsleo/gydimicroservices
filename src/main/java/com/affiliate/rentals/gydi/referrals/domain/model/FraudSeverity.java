package com.affiliate.rentals.gydi.referrals.domain.model;

/**
 * Nivel de severidad de una alerta de fraude
 */
public enum FraudSeverity {
    /**
     * Severidad baja - Comportamiento sospechoso menor
     */
    LOW,

    /**
     * Severidad media - Comportamiento sospechoso moderado
     */
    MEDIUM,

    /**
     * Severidad alta - Comportamiento muy sospechoso
     */
    HIGH,

    /**
     * Severidad crítica - Fraude confirmado o altamente probable
     */
    CRITICAL
}