package com.affiliate.rentals.gydi.referrals.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Modelo de dominio para comisión de afiliado
 *
 * Representa una entrada IMMUTABLE en el ledger de comisiones.
 * Una vez creada, solo se puede cambiar el estado, no los montos.
 *
 * Cumple con requisitos de auditoría financiera y regulaciones contables.
 */
public class Commission {

    private Long id;
    private Long bookingId; // FK a referrals.booking
    private BigDecimal commissionRate; // 0.02 (2%), 0.05 (5%), 0.15 (15%)
    private BigDecimal commissionAmount;
    private String affiliatePlan; // FREE, PRO, ELITE
    private CommissionStatus status;
    private byte[] verificationHash; // SHA-256 para detección de manipulación
    private LocalDateTime createdAt;

    // Constructor privado
    private Commission() {
        this.status = CommissionStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Método de fábrica para crear una nueva comisión
     *
     * @param bookingId ID del booking asociado
     * @param commissionAmount Monto de comisión directa (ya calculado)
     * @param commissionRate Tasa de comisión aplicada
     * @param affiliatePlan Plan del afiliado (FREE, PRO, ELITE)
     */
    public static Commission create(Long bookingId,
            BigDecimal commissionAmount,
            BigDecimal commissionRate,
            String affiliatePlan) {
        if (bookingId == null || bookingId <= 0) {
            throw new IllegalArgumentException("BookingId must be positive");
        }
        if (commissionAmount == null || commissionAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Commission amount must be positive");
        }
        if (commissionRate == null || commissionRate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Commission rate must be positive");
        }
        if (affiliatePlan == null || affiliatePlan.isBlank()) {
            throw new IllegalArgumentException("Affiliate plan cannot be empty");
        }

        Commission commission = new Commission();

        commission.bookingId = bookingId;
        commission.commissionAmount = commissionAmount.setScale(2, RoundingMode.HALF_UP);
        commission.commissionRate = commissionRate;
        commission.affiliatePlan = affiliatePlan;

        return commission;
    }

    /**
     * Verifica si la comisión está en período de hold (30 días desde creación)
     */
    public boolean isInHoldPeriod() {
        LocalDateTime holdEndDate = createdAt.plusDays(30);
        return LocalDateTime.now().isBefore(holdEndDate);
    }

    /**
     * Verifica si la comisión está lista para aprobación
     */
    public boolean isReadyForApproval() {
        return status == CommissionStatus.PENDING && !isInHoldPeriod();
    }

    /**
     * Verifica si la comisión puede ser pagada
     */
    public boolean isPayable() {
        return status == CommissionStatus.APPROVED;
    }

    /**
     * Aprueba la comisión
     */
    public void approve() {
        if (status != CommissionStatus.PENDING) {
            throw new IllegalStateException(
                    "Only PENDING commissions can be approved. Current status: " + status);
        }
        if (isInHoldPeriod()) {
            throw new IllegalStateException(
                    "Cannot approve commission during 30-day hold period");
        }
        this.status = CommissionStatus.APPROVED;
    }

    /**
     * Rechaza la comisión
     */
    public void reject(String reason) {
        if (status == CommissionStatus.PAID) {
            throw new IllegalStateException("Cannot reject a paid commission");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Rejection reason is required");
        }
        this.status = CommissionStatus.REJECTED;
    }

    /**
     * Marca la comisión como pagada
     */
    public void markAsPaid() {
        if (status != CommissionStatus.APPROVED) {
            throw new IllegalStateException(
                    "Only APPROVED commissions can be paid. Current status: " + status);
        }
        this.status = CommissionStatus.PAID;
    }

    /**
     * Obtiene el número de días restantes en período de hold
     */
    public long getRemainingHoldDays() {
        if (!isInHoldPeriod()) {
            return 0;
        }
        LocalDateTime holdEndDate = createdAt.plusDays(30);
        return java.time.Duration.between(LocalDateTime.now(), holdEndDate).toDays();
    }

    /**
     * Verifica la integridad del registro usando el hash de verificación
     */
    public boolean verifyIntegrity(byte[] expectedHash) {
        if (verificationHash == null || expectedHash == null) {
            return false;
        }
        if (verificationHash.length != expectedHash.length) {
            return false;
        }
        for (int i = 0; i < verificationHash.length; i++) {
            if (verificationHash[i] != expectedHash[i]) {
                return false;
            }
        }
        return true;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public BigDecimal getCommissionRate() {
        return commissionRate;
    }

    public BigDecimal getCommissionAmount() {
        return commissionAmount;
    }

    public String getAffiliatePlan() {
        return affiliatePlan;
    }

    public CommissionStatus getStatus() {
        return status;
    }

    public byte[] getVerificationHash() {
        return verificationHash;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // Setters para reconstrucción desde persistencia
    public void setId(Long id) {
        this.id = id;
    }

    public void setStatus(CommissionStatus status) {
        this.status = status;
    }

    public void setVerificationHash(byte[] verificationHash) {
        this.verificationHash = verificationHash;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}