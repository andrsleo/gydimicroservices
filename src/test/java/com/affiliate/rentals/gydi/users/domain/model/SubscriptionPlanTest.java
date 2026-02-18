package com.affiliate.rentals.gydi.users.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Unit tests for {@link SubscriptionPlan}.
 *
 * <p>This test class validates the subscription plan business logic, including:</p>
 * <ul>
 *   <li>Monthly pricing for each plan</li>
 *   <li>Commission rate calculations</li>
 *   <li>Resource limits (properties and referrals)</li>
 *   <li>Plan upgrade logic</li>
 *   <li>Unlimited resource detection</li>
 *   <li>Plan comparison operations</li>
 * </ul>
 *
 * @author GYDI Development Team
 */
@DisplayName("SubscriptionPlan Domain Model Tests")
class SubscriptionPlanTest {

    // ========== Pricing Tests ==========

    @Test
    @DisplayName("FREE plan should have zero monthly price")
    void freePlanShouldHaveZeroMonthlyPrice() {
        // Act
        BigDecimal price = SubscriptionPlan.FREE.getMonthlyPrice();

        // Assert
        assertThat(price).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("PRO plan should have $29.99 monthly price")
    void proPlanShouldHave2999MonthlyPrice() {
        // Act
        BigDecimal price = SubscriptionPlan.PRO.getMonthlyPrice();

        // Assert
        assertThat(price).isEqualByComparingTo(new BigDecimal("29.99"));
    }

    @Test
    @DisplayName("ELITE plan should have $99.99 monthly price")
    void elitePlanShouldHave9999MonthlyPrice() {
        // Act
        BigDecimal price = SubscriptionPlan.ELITE.getMonthlyPrice();

        // Assert
        assertThat(price).isEqualByComparingTo(new BigDecimal("99.99"));
    }

    // ========== Commission Rate Tests ==========

    @Test
    @DisplayName("FREE plan should have 2% commission rate")
    void freePlanShouldHave2PercentCommission() {
        // Act
        BigDecimal rate = SubscriptionPlan.FREE.getCommissionRate();

        // Assert
        assertThat(rate).isEqualByComparingTo(new BigDecimal("0.02"));
    }

    @Test
    @DisplayName("PRO plan should have 5% commission rate")
    void proPlanShouldHave5PercentCommission() {
        // Act
        BigDecimal rate = SubscriptionPlan.PRO.getCommissionRate();

        // Assert
        assertThat(rate).isEqualByComparingTo(new BigDecimal("0.05"));
    }

    @Test
    @DisplayName("ELITE plan should have 10% commission rate")
    void elitePlanShouldHave10PercentCommission() {
        // Act
        BigDecimal rate = SubscriptionPlan.ELITE.getCommissionRate();

        // Assert
        assertThat(rate).isEqualByComparingTo(new BigDecimal("0.10"));
    }

    @ParameterizedTest
    @CsvSource({
        "FREE, 0.02, 2",
        "PRO, 0.05, 5",
        "ELITE, 0.10, 10"
    })
    @DisplayName("Commission rate calculation should be correct")
    void commissionRateCalculationShouldBeCorrect(SubscriptionPlan plan, String rate, int percentage) {
        // Act
        BigDecimal commissionRate = plan.getCommissionRate();
        BigDecimal calculatedPercentage = commissionRate.multiply(new BigDecimal("100"));

        // Assert
        assertThat(commissionRate).isEqualByComparingTo(new BigDecimal(rate));
        assertThat(calculatedPercentage).isEqualByComparingTo(new BigDecimal(percentage));
    }

    // ========== Property Limit Tests ==========

    @Test
    @DisplayName("FREE plan should have unlimited properties (limits disabled)")
    void freePlanShouldHaveUnlimitedPropertiesNow() {
        // Act
        int limit = SubscriptionPlan.FREE.getPropertyPublishLimit();

        // Assert - Límites desactivados temporalmente
        assertThat(limit).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    @DisplayName("PRO plan should have unlimited properties (limits disabled)")
    void proPlanShouldHaveUnlimitedPropertiesNow() {
        // Act
        int limit = SubscriptionPlan.PRO.getPropertyPublishLimit();

        // Assert - Límites desactivados temporalmente
        assertThat(limit).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    @DisplayName("ELITE plan should have unlimited properties")
    void elitePlanShouldHaveUnlimitedProperties() {
        // Act
        int limit = SubscriptionPlan.ELITE.getPropertyPublishLimit();

        // Assert
        assertThat(limit).isEqualTo(Integer.MAX_VALUE);
    }

    // ========== Referral Limit Tests ==========

    @Test
    @DisplayName("FREE plan should have unlimited referrals (limits disabled)")
    void freePlanShouldHaveUnlimitedReferralsNow() {
        // Act
        int limit = SubscriptionPlan.FREE.getReferralGenerateLimit();

        // Assert - Límites desactivados temporalmente
        assertThat(limit).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    @DisplayName("PRO plan should have unlimited referrals (limits disabled)")
    void proPlanShouldHaveUnlimitedReferralsNow() {
        // Act
        int limit = SubscriptionPlan.PRO.getReferralGenerateLimit();

        // Assert - Límites desactivados temporalmente
        assertThat(limit).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    @DisplayName("ELITE plan should have unlimited referrals")
    void elitePlanShouldHaveUnlimitedReferrals() {
        // Act
        int limit = SubscriptionPlan.ELITE.getReferralGenerateLimit();

        // Assert
        assertThat(limit).isEqualTo(Integer.MAX_VALUE);
    }

    // ========== Plan Comparison Tests ==========

    @Test
    @DisplayName("FREE should be lower than PRO")
    void freeShouldBeLowerThanPro() {
        // Act & Assert
        assertThat(SubscriptionPlan.FREE.isLowerThan(SubscriptionPlan.PRO)).isTrue();
    }

    @Test
    @DisplayName("FREE should be lower than ELITE")
    void freeShouldBeLowerThanElite() {
        // Act & Assert
        assertThat(SubscriptionPlan.FREE.isLowerThan(SubscriptionPlan.ELITE)).isTrue();
    }

    @Test
    @DisplayName("PRO should be lower than ELITE")
    void proShouldBeLowerThanElite() {
        // Act & Assert
        assertThat(SubscriptionPlan.PRO.isLowerThan(SubscriptionPlan.ELITE)).isTrue();
    }

    @Test
    @DisplayName("PRO should NOT be lower than FREE")
    void proShouldNotBeLowerThanFree() {
        // Act & Assert
        assertThat(SubscriptionPlan.PRO.isLowerThan(SubscriptionPlan.FREE)).isFalse();
    }

    @Test
    @DisplayName("ELITE should NOT be lower than PRO")
    void eliteShouldNotBeLowerThanPro() {
        // Act & Assert
        assertThat(SubscriptionPlan.ELITE.isLowerThan(SubscriptionPlan.PRO)).isFalse();
    }

    @Test
    @DisplayName("Plan should NOT be lower than itself")
    void planShouldNotBeLowerThanItself() {
        // Act & Assert
        assertThat(SubscriptionPlan.FREE.isLowerThan(SubscriptionPlan.FREE)).isFalse();
        assertThat(SubscriptionPlan.PRO.isLowerThan(SubscriptionPlan.PRO)).isFalse();
        assertThat(SubscriptionPlan.ELITE.isLowerThan(SubscriptionPlan.ELITE)).isFalse();
    }

    // ========== Upgrade Logic Tests ==========

    @Test
    @DisplayName("FREE can upgrade to PRO")
    void freeCanUpgradeToPro() {
        // Act & Assert
        assertThat(SubscriptionPlan.FREE.canUpgradeTo(SubscriptionPlan.PRO)).isTrue();
    }

    @Test
    @DisplayName("FREE can upgrade to ELITE")
    void freeCanUpgradeToElite() {
        // Act & Assert
        assertThat(SubscriptionPlan.FREE.canUpgradeTo(SubscriptionPlan.ELITE)).isTrue();
    }

    @Test
    @DisplayName("PRO can upgrade to ELITE")
    void proCanUpgradeToElite() {
        // Act & Assert
        assertThat(SubscriptionPlan.PRO.canUpgradeTo(SubscriptionPlan.ELITE)).isTrue();
    }

    @Test
    @DisplayName("PRO cannot upgrade to FREE (downgrade)")
    void proCannotUpgradeToFree() {
        // Act & Assert
        assertThat(SubscriptionPlan.PRO.canUpgradeTo(SubscriptionPlan.FREE)).isFalse();
    }

    @Test
    @DisplayName("ELITE cannot upgrade to PRO (downgrade)")
    void eliteCannotUpgradeToPro() {
        // Act & Assert
        assertThat(SubscriptionPlan.ELITE.canUpgradeTo(SubscriptionPlan.PRO)).isFalse();
    }

    @Test
    @DisplayName("Plan cannot upgrade to itself")
    void planCannotUpgradeToItself() {
        // Act & Assert
        assertThat(SubscriptionPlan.FREE.canUpgradeTo(SubscriptionPlan.FREE)).isFalse();
        assertThat(SubscriptionPlan.PRO.canUpgradeTo(SubscriptionPlan.PRO)).isFalse();
        assertThat(SubscriptionPlan.ELITE.canUpgradeTo(SubscriptionPlan.ELITE)).isFalse();
    }

    // ========== Unlimited Resources Tests ==========
    // NOTA: Límites desactivados temporalmente - Todos los planes tienen unlimited

    @Test
    @DisplayName("FREE should have unlimited properties (limits disabled)")
    void freeShouldHaveUnlimitedProperties() {
        // Act & Assert - Límites desactivados temporalmente
        assertThat(SubscriptionPlan.FREE.hasUnlimitedProperties()).isTrue();
    }

    @Test
    @DisplayName("PRO should have unlimited properties (limits disabled)")
    void proShouldHaveUnlimitedProperties() {
        // Act & Assert - Límites desactivados temporalmente
        assertThat(SubscriptionPlan.PRO.hasUnlimitedProperties()).isTrue();
    }

    @Test
    @DisplayName("ELITE should have unlimited properties")
    void eliteShouldHaveUnlimitedProperties() {
        // Act & Assert
        assertThat(SubscriptionPlan.ELITE.hasUnlimitedProperties()).isTrue();
    }

    @Test
    @DisplayName("FREE should have unlimited referrals (limits disabled)")
    void freeShouldHaveUnlimitedReferrals() {
        // Act & Assert - Límites desactivados temporalmente
        assertThat(SubscriptionPlan.FREE.hasUnlimitedReferrals()).isTrue();
    }

    @Test
    @DisplayName("PRO should have unlimited referrals (limits disabled)")
    void proShouldHaveUnlimitedReferrals() {
        // Act & Assert - Límites desactivados temporalmente
        assertThat(SubscriptionPlan.PRO.hasUnlimitedReferrals()).isTrue();
    }

    @Test
    @DisplayName("ELITE should have unlimited referrals")
    void eliteShouldHaveUnlimitedReferrals() {
        // Act & Assert
        assertThat(SubscriptionPlan.ELITE.hasUnlimitedReferrals()).isTrue();
    }

    // ========== Limits Description Tests ==========
    // NOTA: Límites desactivados temporalmente - Todos muestran "Unlimited"

    @Test
    @DisplayName("FREE plan limits description should show unlimited (limits disabled)")
    void freePlanLimitsDescriptionShouldBeCorrect() {
        // Act
        String description = SubscriptionPlan.FREE.getLimitsDescription();

        // Assert - Límites desactivados temporalmente
        assertThat(description).contains("Properties: Unlimited");
        assertThat(description).contains("Referrals: Unlimited");
        assertThat(description).contains("Commission: 2.00%");
    }

    @Test
    @DisplayName("PRO plan limits description should show unlimited (limits disabled)")
    void proPlanLimitsDescriptionShouldBeCorrect() {
        // Act
        String description = SubscriptionPlan.PRO.getLimitsDescription();

        // Assert - Límites desactivados temporalmente
        assertThat(description).contains("Properties: Unlimited");
        assertThat(description).contains("Referrals: Unlimited");
        assertThat(description).contains("Commission: 5.00%");
    }

    @Test
    @DisplayName("ELITE plan limits description should show unlimited")
    void elitePlanLimitsDescriptionShouldShowUnlimited() {
        // Act
        String description = SubscriptionPlan.ELITE.getLimitsDescription();

        // Assert
        assertThat(description).contains("Properties: Unlimited");
        assertThat(description).contains("Referrals: Unlimited");
        assertThat(description).contains("Commission: 10.00%");
    }

    // ========== Enum Behavior Tests ==========

    @Test
    @DisplayName("Should have exactly 3 subscription plans")
    void shouldHaveExactly3SubscriptionPlans() {
        // Act
        SubscriptionPlan[] plans = SubscriptionPlan.values();

        // Assert
        assertThat(plans).hasSize(3);
        assertThat(plans).containsExactly(
            SubscriptionPlan.FREE,
            SubscriptionPlan.PRO,
            SubscriptionPlan.ELITE
        );
    }

    @Test
    @DisplayName("Enum ordinal should reflect plan hierarchy")
    void enumOrdinalShouldReflectPlanHierarchy() {
        // Act & Assert
        assertThat(SubscriptionPlan.FREE.ordinal()).isEqualTo(0);
        assertThat(SubscriptionPlan.PRO.ordinal()).isEqualTo(1);
        assertThat(SubscriptionPlan.ELITE.ordinal()).isEqualTo(2);
    }

    @Test
    @DisplayName("Enum valueOf should work correctly")
    void enumValueOfShouldWorkCorrectly() {
        // Act & Assert
        assertThat(SubscriptionPlan.valueOf("FREE")).isEqualTo(SubscriptionPlan.FREE);
        assertThat(SubscriptionPlan.valueOf("PRO")).isEqualTo(SubscriptionPlan.PRO);
        assertThat(SubscriptionPlan.valueOf("ELITE")).isEqualTo(SubscriptionPlan.ELITE);
    }

    // ========== Business Logic Validation Tests ==========

    @Test
    @DisplayName("Higher plans should always have higher or equal limits (all unlimited now)")
    void higherPlansShouldAlwaysHaveHigherOrEqualLimits() {
        // Act & Assert - Property limits (todos unlimited actualmente)
        assertThat(SubscriptionPlan.PRO.getPropertyPublishLimit())
            .isGreaterThanOrEqualTo(SubscriptionPlan.FREE.getPropertyPublishLimit());
        assertThat(SubscriptionPlan.ELITE.getPropertyPublishLimit())
            .isGreaterThanOrEqualTo(SubscriptionPlan.PRO.getPropertyPublishLimit());

        // Act & Assert - Referral limits (todos unlimited actualmente)
        assertThat(SubscriptionPlan.PRO.getReferralGenerateLimit())
            .isGreaterThanOrEqualTo(SubscriptionPlan.FREE.getReferralGenerateLimit());
        assertThat(SubscriptionPlan.ELITE.getReferralGenerateLimit())
            .isGreaterThanOrEqualTo(SubscriptionPlan.PRO.getReferralGenerateLimit());
    }

    @Test
    @DisplayName("Higher plans should always have higher or equal commission rates")
    void higherPlansShouldAlwaysHaveHigherOrEqualCommissionRates() {
        // Act & Assert
        assertThat(SubscriptionPlan.PRO.getCommissionRate())
            .isGreaterThan(SubscriptionPlan.FREE.getCommissionRate());
        assertThat(SubscriptionPlan.ELITE.getCommissionRate())
            .isGreaterThan(SubscriptionPlan.PRO.getCommissionRate());
    }

    @Test
    @DisplayName("Higher plans should always have higher or equal prices")
    void higherPlansShouldAlwaysHaveHigherOrEqualPrices() {
        // Act & Assert
        assertThat(SubscriptionPlan.PRO.getMonthlyPrice())
            .isGreaterThan(SubscriptionPlan.FREE.getMonthlyPrice());
        assertThat(SubscriptionPlan.ELITE.getMonthlyPrice())
            .isGreaterThan(SubscriptionPlan.PRO.getMonthlyPrice());
    }
}