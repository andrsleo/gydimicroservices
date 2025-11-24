package com.affiliate.rentals.gydi.referrals.domain.model;

/**
 * Estado de la comisión en el proceso de pago
 */
public enum CommissionStatus {
    /**
     * Comisión pendiente de aprobación (período de hold de 30 días)
     */
    PENDING,

    /**
     * Comisión aprobada y lista para pago
     */
    APPROVED,

    /**
     * Comisión rechazada (fraude, cancelación, etc.)
     */
    REJECTED,

    /**
     * Comisión pagada al afiliado
     */
    PAID
}