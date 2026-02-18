package com.affiliate.rentals.gydi.commissions.infrastructure.out.persistence.repository;

import com.affiliate.rentals.gydi.commissions.infrastructure.out.persistence.entity.HostCommissionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface HostCommissionJpaRepository extends JpaRepository<HostCommissionJpaEntity, Long> {
    Optional<HostCommissionJpaEntity> findByBookingId(Long bookingId);
    List<HostCommissionJpaEntity> findByHostId(Long hostId);
    List<HostCommissionJpaEntity> findByHostIdAndStatus(Long hostId, String status);
    List<HostCommissionJpaEntity> findByStatus(String status);
    boolean existsByBookingId(Long bookingId);
    
    @Query("SELECT h FROM HostCommissionJpaEntity h WHERE h.status = 'FAILED' AND h.nextRetryAt <= :now")
    List<HostCommissionJpaEntity> findCommissionsForRetry(@Param("now") LocalDateTime now);
}
