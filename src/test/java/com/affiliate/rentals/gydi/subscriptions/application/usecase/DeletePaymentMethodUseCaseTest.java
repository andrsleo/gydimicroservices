package com.affiliate.rentals.gydi.subscriptions.application.usecase;

import com.affiliate.rentals.gydi.subscriptions.domain.exception.CannotDeletePaymentMethodException;
import com.affiliate.rentals.gydi.subscriptions.domain.exception.PaymentGatewayException;
import com.affiliate.rentals.gydi.subscriptions.domain.exception.PaymentMethodNotFoundException;
import com.affiliate.rentals.gydi.subscriptions.domain.exception.PlanNotFoundException;
import com.affiliate.rentals.gydi.subscriptions.domain.model.PaymentMethod;
import com.affiliate.rentals.gydi.subscriptions.domain.model.PaymentMethodType;
import com.affiliate.rentals.gydi.subscriptions.domain.model.Plan;
import com.affiliate.rentals.gydi.subscriptions.domain.model.SubscriptionStatus;
import com.affiliate.rentals.gydi.subscriptions.domain.model.UserSubscription;
import com.affiliate.rentals.gydi.subscriptions.domain.ports.PaymentGatewayPort;
import com.affiliate.rentals.gydi.subscriptions.domain.ports.PaymentGatewayPort.PaymentMethodResult;
import com.affiliate.rentals.gydi.subscriptions.domain.ports.PaymentMethodRepositoryPort;
import com.affiliate.rentals.gydi.subscriptions.domain.ports.PlanRepositoryPort;
import com.affiliate.rentals.gydi.subscriptions.domain.ports.UserSubscriptionRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DeletePaymentMethodUseCase}.
 *
 * <p>
 * This test class validates the payment method soft delete functionality,
 * ensuring proper business rule enforcement, security validation, and
 * correct state management.
 * </p>
 *
 * <p>
 * Business Rules Tested:
 * </p>
 * <ul>
 * <li>Users with paid subscriptions MUST keep at least one payment method</li>
 * <li>Default payment method can only be deleted if it's the only one
 * remaining</li>
 * <li>Only the payment method owner can delete it</li>
 * <li>Payment method is marked as INACTIVE (soft delete) instead of physical
 * deletion</li>
 * <li>Stripe payment method is NOT detached to maintain consistency</li>
 * </ul>
 *
 * @author GYDI Development Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DeletePaymentMethodUseCase Tests")
class DeletePaymentMethodUseCaseTest {

        @Mock
        private PaymentMethodRepositoryPort paymentMethodRepository;

        @Mock
        private UserSubscriptionRepositoryPort subscriptionRepository;

        @Mock
        private PlanRepositoryPort planRepository;

        @Mock
        private PaymentGatewayPort paymentGateway;

        private DeletePaymentMethodUseCase deletePaymentMethodUseCase;

        private static final Long USER_ID = 1L;
        private static final Long OTHER_USER_ID = 2L;
        private static final Long PAYMENT_METHOD_ID = 100L;
        private static final Long SECOND_PAYMENT_METHOD_ID = 101L;
        private static final String GATEWAY_TOKEN = "pm_1234567890";

        private PaymentMethod testPaymentMethod;
        private PaymentMethod defaultPaymentMethod;
        private PaymentMethod secondPaymentMethod;
        private Plan freePlan;
        private Plan proPlan;
        private UserSubscription activeFreeSubscription;
        private UserSubscription activeProSubscription;
        private PaymentMethodResult mockPaymentMethodResult;

        @BeforeEach
        void setUp() {
                // Initialize use case with Optional.of(paymentGateway) for most tests
                deletePaymentMethodUseCase = new DeletePaymentMethodUseCase(
                                paymentMethodRepository,
                                subscriptionRepository,
                                planRepository,
                                Optional.of(paymentGateway));

                // Create test payment methods
                testPaymentMethod = PaymentMethod.builder()
                                .id(PAYMENT_METHOD_ID)
                                .userId(USER_ID)
                                .methodType(PaymentMethodType.CREDIT_CARD)
                                .gatewayProvider("stripe")
                                .gatewayToken(GATEWAY_TOKEN)
                                .cardLastFour("4242")
                                .cardBrand("visa")
                                .cardExpMonth(12)
                                .cardExpYear(2028)
                                .billingEmail("user@example.com")
                                .isDefault(false)
                                .build();

                defaultPaymentMethod = PaymentMethod.builder()
                                .id(PAYMENT_METHOD_ID)
                                .userId(USER_ID)
                                .methodType(PaymentMethodType.CREDIT_CARD)
                                .gatewayProvider("stripe")
                                .gatewayToken(GATEWAY_TOKEN)
                                .cardLastFour("4242")
                                .cardBrand("visa")
                                .cardExpMonth(12)
                                .cardExpYear(2028)
                                .billingEmail("user@example.com")
                                .isDefault(true)
                                .build();

                secondPaymentMethod = PaymentMethod.builder()
                                .id(SECOND_PAYMENT_METHOD_ID)
                                .userId(USER_ID)
                                .methodType(PaymentMethodType.CREDIT_CARD)
                                .gatewayProvider("stripe")
                                .gatewayToken("pm_9876543210")
                                .cardLastFour("5555")
                                .cardBrand("mastercard")
                                .cardExpMonth(6)
                                .cardExpYear(2029)
                                .billingEmail("user@example.com")
                                .isDefault(false)
                                .build();

                // Create test plans
                freePlan = Plan.builder()
                                .id(1L)
                                .planCode("FREE")
                                .planName("Free Plan")
                                .monthlyPrice(BigDecimal.ZERO)
                                .commissionRate(new BigDecimal("0.02"))
                                .referralLimitPerMonth(10)
                                .propertyPublishLimit(5)
                                .build();

                proPlan = Plan.builder()
                                .id(2L)
                                .planCode("PRO")
                                .planName("Pro Plan")
                                .monthlyPrice(new BigDecimal("29.00"))
                                .commissionRate(new BigDecimal("0.05"))
                                .referralLimitPerMonth(50)
                                .propertyPublishLimit(50)
                                .build();

                // Create test subscriptions
                activeFreeSubscription = UserSubscription.builder()
                                .id(1L)
                                .userId(USER_ID)
                                .planId(freePlan.id())
                                .status(SubscriptionStatus.ACTIVE)
                                .paymentMethodId(PAYMENT_METHOD_ID)
                                .build();

                activeProSubscription = UserSubscription.builder()
                                .id(2L)
                                .userId(USER_ID)
                                .planId(proPlan.id())
                                .status(SubscriptionStatus.ACTIVE)
                                .paymentMethodId(PAYMENT_METHOD_ID)
                                .build();

                // Create mock PaymentMethodResult for successful detachment
                mockPaymentMethodResult = new PaymentMethodResult(
                                GATEWAY_TOKEN,
                                "card",
                                "visa",
                                "4242",
                                12,
                                2028);
        }

        @Test
        @DisplayName("Should soft delete payment method successfully when all validations pass")
        void shouldDeletePaymentMethodSuccessfully() {
                // Arrange
                when(paymentMethodRepository.findById(PAYMENT_METHOD_ID)).thenReturn(Optional.of(testPaymentMethod));
                when(paymentMethodRepository.countActiveByUserId(USER_ID)).thenReturn(2L);
                when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.of(activeProSubscription));
                when(planRepository.findById(proPlan.id())).thenReturn(Optional.of(proPlan));
                when(paymentMethodRepository.save(any(PaymentMethod.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                // Act & Assert
                assertThatCode(() -> deletePaymentMethodUseCase.execute(PAYMENT_METHOD_ID, USER_ID))
                                .doesNotThrowAnyException();

                // Verify interactions
                verify(paymentMethodRepository).findById(PAYMENT_METHOD_ID);
                verify(paymentMethodRepository).countActiveByUserId(USER_ID);
                verify(subscriptionRepository).findByUserId(USER_ID);
                verify(planRepository).findById(proPlan.id());
                verify(paymentMethodRepository).save(any(PaymentMethod.class));
                verify(paymentMethodRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Should throw PaymentMethodNotFoundException when payment method does not exist")
        void shouldThrowExceptionWhenPaymentMethodNotFound() {
                // Arrange
                when(paymentMethodRepository.findById(PAYMENT_METHOD_ID)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> deletePaymentMethodUseCase.execute(PAYMENT_METHOD_ID, USER_ID))
                                .isInstanceOf(PaymentMethodNotFoundException.class)
                                .hasMessageContaining(String.valueOf(PAYMENT_METHOD_ID));

                verify(paymentMethodRepository).findById(PAYMENT_METHOD_ID);
                verify(paymentMethodRepository, never()).save(any());
                verify(paymentMethodRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Should throw CannotDeletePaymentMethodException when payment method belongs to another user")
        void shouldThrowExceptionWhenPaymentMethodBelongsToOtherUser() {
                // Arrange
                PaymentMethod otherUserPaymentMethod = PaymentMethod.builder()
                                .id(PAYMENT_METHOD_ID)
                                .userId(OTHER_USER_ID)
                                .methodType(PaymentMethodType.CREDIT_CARD)
                                .gatewayProvider("stripe")
                                .gatewayToken(GATEWAY_TOKEN)
                                .cardLastFour("4242")
                                .cardBrand("visa")
                                .cardExpMonth(12)
                                .cardExpYear(2028)
                                .build();

                when(paymentMethodRepository.findById(PAYMENT_METHOD_ID))
                                .thenReturn(Optional.of(otherUserPaymentMethod));

                // Act & Assert
                assertThatThrownBy(() -> deletePaymentMethodUseCase.execute(PAYMENT_METHOD_ID, USER_ID))
                                .isInstanceOf(CannotDeletePaymentMethodException.class)
                                .hasMessageContaining("not found or does not belong to you");

                verify(paymentMethodRepository).findById(PAYMENT_METHOD_ID);
                verify(paymentMethodRepository, never()).save(any());
                verify(paymentMethodRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Should throw CannotDeletePaymentMethodException when deleting only method with paid subscription")
        void shouldThrowExceptionWhenDeletingOnlyMethodWithPaidSubscription() {
                // Arrange
                when(paymentMethodRepository.findById(PAYMENT_METHOD_ID)).thenReturn(Optional.of(testPaymentMethod));
                when(paymentMethodRepository.countActiveByUserId(USER_ID)).thenReturn(1L); // Only one method
                when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.of(activeProSubscription));
                when(planRepository.findById(proPlan.id())).thenReturn(Optional.of(proPlan));

                // Act & Assert
                assertThatThrownBy(() -> deletePaymentMethodUseCase.execute(PAYMENT_METHOD_ID, USER_ID))
                                .isInstanceOf(CannotDeletePaymentMethodException.class)
                                .hasMessageContaining("only payment method")
                                .hasMessageContaining("paid subscription");

                verify(paymentMethodRepository).findById(PAYMENT_METHOD_ID);
                verify(paymentMethodRepository).countActiveByUserId(USER_ID);
                verify(subscriptionRepository).findByUserId(USER_ID);
                verify(planRepository).findById(proPlan.id());
                verify(paymentMethodRepository, never()).save(any());
                verify(paymentMethodRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Should allow soft deleting only payment method when user has free subscription")
        void shouldAllowDeletingOnlyMethodWithFreeSubscription() {
                // Arrange
                when(paymentMethodRepository.findById(PAYMENT_METHOD_ID)).thenReturn(Optional.of(testPaymentMethod));
                when(paymentMethodRepository.countActiveByUserId(USER_ID)).thenReturn(1L); // Only one method
                when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.of(activeFreeSubscription));
                when(planRepository.findById(freePlan.id())).thenReturn(Optional.of(freePlan));
                when(paymentMethodRepository.save(any(PaymentMethod.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                // Act & Assert
                assertThatCode(() -> deletePaymentMethodUseCase.execute(PAYMENT_METHOD_ID, USER_ID))
                                .doesNotThrowAnyException();

                verify(paymentMethodRepository).findById(PAYMENT_METHOD_ID);
                verify(paymentMethodRepository).countActiveByUserId(USER_ID);
                verify(subscriptionRepository).findByUserId(USER_ID);
                verify(planRepository).findById(freePlan.id());
                verify(paymentMethodRepository).save(any(PaymentMethod.class));
                verify(paymentMethodRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Should allow soft deleting only payment method when user has no subscription")
        void shouldAllowDeletingOnlyMethodWithNoSubscription() {
                // Arrange
                when(paymentMethodRepository.findById(PAYMENT_METHOD_ID)).thenReturn(Optional.of(testPaymentMethod));
                when(paymentMethodRepository.countActiveByUserId(USER_ID)).thenReturn(1L);
                when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.empty()); // No subscription
                when(paymentMethodRepository.save(any(PaymentMethod.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                // Act & Assert
                assertThatCode(() -> deletePaymentMethodUseCase.execute(PAYMENT_METHOD_ID, USER_ID))
                                .doesNotThrowAnyException();

                verify(paymentMethodRepository).findById(PAYMENT_METHOD_ID);
                verify(paymentMethodRepository).countActiveByUserId(USER_ID);
                verify(subscriptionRepository).findByUserId(USER_ID);
                verify(planRepository, never()).findById(any());
                verify(paymentMethodRepository).save(any(PaymentMethod.class));
                verify(paymentMethodRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Should throw CannotDeletePaymentMethodException when deleting default method with other methods present")
        void shouldThrowExceptionWhenDeletingDefaultWithOtherMethods() {
                // Arrange
                when(paymentMethodRepository.findById(PAYMENT_METHOD_ID)).thenReturn(Optional.of(defaultPaymentMethod));
                when(paymentMethodRepository.countActiveByUserId(USER_ID)).thenReturn(2L); // Multiple methods
                when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.of(activeProSubscription));
                when(planRepository.findById(proPlan.id())).thenReturn(Optional.of(proPlan));

                // Act & Assert
                assertThatThrownBy(() -> deletePaymentMethodUseCase.execute(PAYMENT_METHOD_ID, USER_ID))
                                .isInstanceOf(CannotDeletePaymentMethodException.class)
                                .hasMessageContaining("default payment method");

                verify(paymentMethodRepository).findById(PAYMENT_METHOD_ID);
                verify(paymentMethodRepository).countActiveByUserId(USER_ID);
                verify(subscriptionRepository).findByUserId(USER_ID);
                verify(planRepository).findById(proPlan.id());
                verify(paymentMethodRepository, never()).save(any());
                verify(paymentMethodRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Should allow soft deleting default payment method when it's the only one")
        void shouldAllowDeletingDefaultWhenOnlyMethod() {
                // Arrange
                when(paymentMethodRepository.findById(PAYMENT_METHOD_ID)).thenReturn(Optional.of(defaultPaymentMethod));
                when(paymentMethodRepository.countActiveByUserId(USER_ID)).thenReturn(1L); // Only one method
                when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.of(activeFreeSubscription));
                when(planRepository.findById(freePlan.id())).thenReturn(Optional.of(freePlan));
                when(paymentMethodRepository.save(any(PaymentMethod.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                // Act & Assert
                assertThatCode(() -> deletePaymentMethodUseCase.execute(PAYMENT_METHOD_ID, USER_ID))
                                .doesNotThrowAnyException();

                verify(paymentMethodRepository).findById(PAYMENT_METHOD_ID);
                verify(paymentMethodRepository).countActiveByUserId(USER_ID);
                verify(subscriptionRepository).findByUserId(USER_ID);
                verify(planRepository).findById(freePlan.id());
                verify(paymentMethodRepository).save(any(PaymentMethod.class));
                verify(paymentMethodRepository, never()).deleteById(any());
        }

        // Test removed: Stripe detachment is no longer performed for soft delete
        // Original test: shouldRollbackWhenStripeDetachmentFails
        // Rationale: Soft delete keeps payment method in DB and doesn't detach from
        // Stripe

        @Test
        @DisplayName("Should throw PlanNotFoundException when subscription plan does not exist")
        void shouldThrowExceptionWhenPlanNotFound() {
                // Arrange
                when(paymentMethodRepository.findById(PAYMENT_METHOD_ID)).thenReturn(Optional.of(testPaymentMethod));
                when(paymentMethodRepository.countActiveByUserId(USER_ID)).thenReturn(1L);
                when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.of(activeProSubscription));
                when(planRepository.findById(proPlan.id())).thenReturn(Optional.empty()); // Plan not found

                // Act & Assert
                assertThatThrownBy(() -> deletePaymentMethodUseCase.execute(PAYMENT_METHOD_ID, USER_ID))
                                .isInstanceOf(PlanNotFoundException.class)
                                .hasMessageContaining(String.valueOf(proPlan.id()));

                verify(paymentMethodRepository).findById(PAYMENT_METHOD_ID);
                verify(paymentMethodRepository).countActiveByUserId(USER_ID);
                verify(subscriptionRepository).findByUserId(USER_ID);
                verify(planRepository).findById(proPlan.id());
                verify(paymentMethodRepository, never()).save(any());
                verify(paymentMethodRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Should perform soft delete regardless of payment gateway availability")
        void shouldLogWarningWhenPaymentGatewayNotAvailable() {
                // Arrange - Create use case with Optional.empty() for payment gateway
                DeletePaymentMethodUseCase useCaseWithoutGateway = new DeletePaymentMethodUseCase(
                                paymentMethodRepository,
                                subscriptionRepository,
                                planRepository,
                                Optional.empty());

                when(paymentMethodRepository.findById(PAYMENT_METHOD_ID)).thenReturn(Optional.of(testPaymentMethod));
                when(paymentMethodRepository.countActiveByUserId(USER_ID)).thenReturn(2L);
                when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.of(activeProSubscription));
                when(planRepository.findById(proPlan.id())).thenReturn(Optional.of(proPlan));
                when(paymentMethodRepository.save(any(PaymentMethod.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                // Act & Assert
                assertThatCode(() -> useCaseWithoutGateway.execute(PAYMENT_METHOD_ID, USER_ID))
                                .doesNotThrowAnyException();

                verify(paymentMethodRepository).findById(PAYMENT_METHOD_ID);
                verify(paymentMethodRepository).countActiveByUserId(USER_ID);
                verify(subscriptionRepository).findByUserId(USER_ID);
                verify(planRepository).findById(proPlan.id());
                verify(paymentMethodRepository).save(any(PaymentMethod.class));
                // Payment gateway should not be called for soft delete
                verifyNoInteractions(paymentGateway);
        }

        // Test removed: Order of Stripe detachment vs database deletion
        // Original test: shouldDetachFromStripeBeforeDeletingFromDatabase
        // Rationale: Soft delete only updates status, no Stripe interaction needed

        @Test
        @DisplayName("Should call repository methods exactly once for successful soft deletion")
        void shouldCallRepositoryMethodsExactlyOnce() {
                // Arrange
                when(paymentMethodRepository.findById(PAYMENT_METHOD_ID)).thenReturn(Optional.of(testPaymentMethod));
                when(paymentMethodRepository.countActiveByUserId(USER_ID)).thenReturn(2L);
                when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.of(activeProSubscription));
                when(planRepository.findById(proPlan.id())).thenReturn(Optional.of(proPlan));
                when(paymentMethodRepository.save(any(PaymentMethod.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                // Act
                deletePaymentMethodUseCase.execute(PAYMENT_METHOD_ID, USER_ID);

                // Assert
                verify(paymentMethodRepository, times(1)).findById(PAYMENT_METHOD_ID);
                verify(paymentMethodRepository, times(1)).countActiveByUserId(USER_ID);
                verify(subscriptionRepository, times(1)).findByUserId(USER_ID);
                verify(planRepository, times(1)).findById(proPlan.id());
                verify(paymentMethodRepository, times(1)).save(any(PaymentMethod.class));
                verify(paymentMethodRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Should handle null payment method ID gracefully")
        void shouldHandleNullPaymentMethodIdGracefully() {
                // Arrange
                when(paymentMethodRepository.findById(null)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> deletePaymentMethodUseCase.execute(null, USER_ID))
                                .isInstanceOf(PaymentMethodNotFoundException.class);

                verify(paymentMethodRepository).findById(null);
                verify(paymentMethodRepository, never()).save(any());
                verify(paymentMethodRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Should handle null user ID gracefully")
        void shouldHandleNullUserIdGracefully() {
                // Arrange
                when(paymentMethodRepository.findById(PAYMENT_METHOD_ID)).thenReturn(Optional.of(testPaymentMethod));

                // Act & Assert
                assertThatThrownBy(() -> deletePaymentMethodUseCase.execute(PAYMENT_METHOD_ID, null))
                                .isInstanceOf(CannotDeletePaymentMethodException.class)
                                .hasMessageContaining("not found or does not belong to you");

                verify(paymentMethodRepository).findById(PAYMENT_METHOD_ID);
                verify(paymentMethodRepository, never()).save(any());
                verify(paymentMethodRepository, never()).deleteById(any());
        }
}
