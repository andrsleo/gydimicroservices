package com.affiliate.rentals.gydi.commissions.infrastructure.out.persistence.repository;

import com.affiliate.rentals.gydi.commissions.infrastructure.out.persistence.entity.ReferralCommissionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReferralCommissionJpaRepository extends JpaRepository<ReferralCommissionJpaEntity, Long> {
    Optional<ReferralCommissionJpaEntity> findByBookingId(Long bookingId);
    List<ReferralCommissionJpaEntity> findByAffiliateId(Long affiliateId);
    List<ReferralCommissionJpaEntity> findByAffiliateIdAndStatus(Long affiliateId, String status);
    List<ReferralCommissionJpaEntity> findByStatus(String status);
    boolean existsByBookingId(Long bookingId);

    @Query("SELECT r FROM ReferralCommissionJpaEntity r WHERE r.status = 'PENDING' AND r.disputePeriodEndsAt <= :now")
    List<ReferralCommissionJpaEntity> findCommissionsReadyForApproval(@Param("now") LocalDateTime now);

    @Query("SELECT r FROM ReferralCommissionJpaEntity r WHERE r.status = 'APPROVED' AND r.scheduledPaymentDate = :date")
    List<ReferralCommissionJpaEntity> findCommissionsForPayment(@Param("date") LocalDate date);
}
