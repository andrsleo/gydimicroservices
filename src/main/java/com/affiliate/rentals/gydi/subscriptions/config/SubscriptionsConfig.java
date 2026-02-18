package com.affiliate.rentals.gydi.subscriptions.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.affiliate.rentals.gydi.subscriptions.domain.service.CommissionCalculatorService;

import lombok.extern.slf4j.Slf4j;

/**
 * Subscriptions Bounded Context Configuration
 *
 * <p>
 * Configures Spring beans for the Subscriptions bounded context.
 * Registers domain services that have no framework dependencies.
 * </p>
 *
 * <p>
 * <strong>Why this configuration is needed:</strong><br>
 * Domain services in hexagonal architecture are pure business logic
 * with no Spring annotations. They must be manually registered as beans
 * to be injectable into use cases and other Spring-managed components.
 * </p>
 *
 * <p>
 * <strong>Registered Beans:</strong>
 * <ul>
 *   <li>{@link CommissionCalculatorService} - Pure domain service for commission calculations</li>
 * </ul>
 * </p>
 *
 * @see CommissionCalculatorService
 * @author GYDI Development Team
 */
@Slf4j
@Configuration
public class SubscriptionsConfig {

    /**
     * Creates CommissionCalculatorService bean.
     *
     * <p>
     * This domain service calculates commissions with correct semantics:
     * <ul>
     *   <li><strong>AFFILIATE:</strong> RECEIVES commission FROM platform (2%, 5%, 10%)</li>
     *   <li><strong>HOST:</strong> PAYS commission TO platform (25%, 20%, 15%)</li>
     * </ul>
     * </p>
     *
     * <p>
     * <strong>Example:</strong> $100 booking with PRO/PRO plans<br>
     * - Platform charges host: $20 (20%) → Host receives $80<br>
     * - Platform pays affiliate: $5 (5%) → Affiliate receives $5<br>
     * - Platform profit: $15
     * </p>
     *
     * @return configured CommissionCalculatorService instance
     */
    @Bean
    public CommissionCalculatorService commissionCalculatorService() {
        log.info("Initializing CommissionCalculatorService domain service");
        CommissionCalculatorService service = new CommissionCalculatorService();
        log.info("CommissionCalculatorService initialized successfully");
        return service;
    }
}
