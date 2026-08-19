package com.affiliate.rentals.gydi.commissions.config;

import com.affiliate.rentals.gydi.commissions.domain.ports.UserSubscriptionPort;
import com.affiliate.rentals.gydi.commissions.domain.service.CommissionCalculationService;
import com.affiliate.rentals.gydi.subscriptions.domain.service.CommissionCalculatorService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration for commissions bounded context.
 */
@Configuration
@EnableScheduling
public class CommissionConfig {

    /**
     * Domain service for commission calculation.
     *
     * <p><strong>Dependency:</strong> CommissionCalculatorService is autowired from
     * SubscriptionsConfig (subscriptions bounded context). We don't create it here
     * to avoid bean duplication and maintain proper DDD boundaries.</p>
     */
    @Bean
    public CommissionCalculationService commissionCalculationService(
        CommissionCalculatorService calculatorService,
        UserSubscriptionPort userSubscriptionPort
    ) {
        return new CommissionCalculationService(calculatorService, userSubscriptionPort);
    }
}
