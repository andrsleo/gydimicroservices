package com.affiliate.rentals.gydi.shared.audit;

import com.affiliate.rentals.gydi.users.application.dto.ForgotPasswordRequest;
import com.affiliate.rentals.gydi.users.application.dto.ResetPasswordRequest;
import com.affiliate.rentals.gydi.users.infrastructure.out.persistence.entity.PasswordResetAuditEntity;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Aspect for auditing password reset operations.
 *
 * <p>This aspect intercepts password reset use cases and automatically logs
 * all operations to the password_reset_audit table for security tracking and compliance.</p>
 *
 * <p>Audited operations:</p>
 * <ul>
 *   <li>REQUEST - Password reset was requested</li>
 *   <li>VALIDATE - Token validation was performed</li>
 *   <li>RESET - Password was successfully reset</li>
 * </ul>
 *
 * <p>Captured information:</p>
 * <ul>
 *   <li>User email</li>
 *   <li>Action type</li>
 *   <li>Success/failure status</li>
 *   <li>Client IP address</li>
 *   <li>User agent (browser/client information)</li>
 *   <li>Timestamp</li>
 * </ul>
 *
 * <p>The aspect handles exceptions gracefully to ensure that audit logging failures
 * don't prevent the actual operation from completing.</p>
 *
 * @author GYDI Development Team
 */
@Aspect
@Component
public class PasswordResetAuditAspect {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetAuditAspect.class);

    private final PasswordResetAuditService auditService;

    public PasswordResetAuditAspect(PasswordResetAuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * Audits password reset request operations.
     *
     * @param joinPoint the join point
     * @return the result of the method execution
     * @throws Throwable if the method execution fails
     */
    @Around("execution(* com.affiliate.rentals.gydi.users.application.usecase.RequestPasswordResetUseCase.execute(..))")
    public Object auditPasswordResetRequest(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = getCurrentHttpRequest();
        String email = extractEmailFromArgs(joinPoint);
        String ipAddress = extractIpAddress(request);
        String userAgent = extractUserAgent(request);

        boolean success = false;
        try {
            Object result = joinPoint.proceed();
            success = true;
            return result;
        } catch (Exception e) {
            throw e;
        } finally {
            auditService.saveAudit(email, PasswordResetAuditEntity.PasswordResetAction.REQUEST,
                    success, ipAddress, userAgent);
        }
    }

    /**
     * Audits token validation operations.
     *
     * @param joinPoint the join point
     * @return the result of the method execution
     * @throws Throwable if the method execution fails
     */
    @Around("execution(* com.affiliate.rentals.gydi.users.application.usecase.ValidateResetTokenUseCase.execute(..))")
    public Object auditTokenValidation(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = getCurrentHttpRequest();
        String ipAddress = extractIpAddress(request);
        String userAgent = extractUserAgent(request);

        Object result = null;
        boolean success = false;
        String email = "unknown";

        try {
            result = joinPoint.proceed();
            // Extract email from result if validation was successful
            if (result != null) {
                try {
                    var method = result.getClass().getMethod("valid");
                    boolean valid = (boolean) method.invoke(result);
                    success = valid;

                    if (valid) {
                        var emailMethod = result.getClass().getMethod("email");
                        email = (String) emailMethod.invoke(result);
                    }
                } catch (Exception e) {
                    log.debug("Could not extract validation result details: {}", e.getMessage());
                }
            }
            return result;
        } catch (Exception e) {
            throw e;
        } finally {
            auditService.saveAudit(email, PasswordResetAuditEntity.PasswordResetAction.VALIDATE,
                    success, ipAddress, userAgent);
        }
    }

    /**
     * Audits password reset completion operations.
     *
     * @param joinPoint the join point
     * @return the result of the method execution
     * @throws Throwable if the method execution fails
     */
    @Around("execution(* com.affiliate.rentals.gydi.users.application.usecase.ResetPasswordUseCase.execute(..))")
    public Object auditPasswordReset(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = getCurrentHttpRequest();
        String ipAddress = extractIpAddress(request);
        String userAgent = extractUserAgent(request);
        String email = "unknown";

        boolean success = false;
        try {
            Object result = joinPoint.proceed();
            // Extract success status from result
            if (result != null) {
                try {
                    var method = result.getClass().getMethod("success");
                    success = (boolean) method.invoke(result);
                } catch (Exception e) {
                    log.debug("Could not extract reset result success status: {}", e.getMessage());
                }
            }
            return result;
        } catch (Exception e) {
            throw e;
        } finally {
            // Try to extract email from request parameter
            if (joinPoint.getArgs().length > 0 && joinPoint.getArgs()[0] instanceof ResetPasswordRequest) {
                // Email is not in ResetPasswordRequest, will remain unknown for RESET action
                // Alternative: could be enhanced to lookup user by token
            }
            auditService.saveAudit(email, PasswordResetAuditEntity.PasswordResetAction.RESET,
                    success, ipAddress, userAgent);
        }
    }

    /**
     * Extracts email from method arguments (for REQUEST operation).
     *
     * @param joinPoint the join point
     * @return the email address, or "unknown" if not found
     */
    private String extractEmailFromArgs(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg instanceof ForgotPasswordRequest) {
                return ((ForgotPasswordRequest) arg).email();
            }
        }
        return "unknown";
    }

    /**
     * Extracts the client IP address from the HTTP request.
     *
     * @param request the HTTP request
     * @return the IP address, or "unknown" if not available
     */
    private String extractIpAddress(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        String ip = request.getHeader("X-Forwarded-For");

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip != null ? ip : "unknown";
    }

    /**
     * Extracts the user agent from the HTTP request.
     *
     * @param request the HTTP request
     * @return the user agent string, or "unknown" if not available
     */
    private String extractUserAgent(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        String userAgent = request.getHeader("User-Agent");
        return userAgent != null ? userAgent : "unknown";
    }

    /**
     * Gets the current HTTP request from the request context.
     *
     * @return the HttpServletRequest, or null if not available
     */
    private HttpServletRequest getCurrentHttpRequest() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            log.debug("Could not obtain HttpServletRequest: {}", e.getMessage());
            return null;
        }
    }
}
