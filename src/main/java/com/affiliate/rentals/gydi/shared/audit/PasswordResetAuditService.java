package com.affiliate.rentals.gydi.shared.audit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.affiliate.rentals.gydi.users.infrastructure.out.persistence.entity.PasswordResetAuditEntity;
import com.affiliate.rentals.gydi.users.infrastructure.out.persistence.repository.PasswordResetAuditJpaRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PasswordResetAuditService {

    private final PasswordResetAuditJpaRepository auditRepository;

    public PasswordResetAuditService(PasswordResetAuditJpaRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveAudit(String email, PasswordResetAuditEntity.PasswordResetAction action,
                          boolean success, String ipAddress, String userAgent) {
        try {
            PasswordResetAuditEntity audit = new PasswordResetAuditEntity();
            audit.setUserEmail(email != null ? email : "unknown");
            audit.setAction(action);
            audit.setSuccess(success);
            audit.setIpAddress(ipAddress);
            audit.setUserAgent(userAgent);
            auditRepository.save(audit);
            log.debug("Audit saved: action={}, email={}, success={}", action, email, success);
        } catch (Exception e) {
            log.error("Failed to save audit for action {} on email {}: {}", action, email, e.getMessage());
        }
    }
}