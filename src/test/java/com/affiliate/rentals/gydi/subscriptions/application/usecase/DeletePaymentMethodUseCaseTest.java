package com.affiliate.rentals.gydi.subscriptions.application.usecase;

import com.affiliate.rentals.gydi.subscriptions.domain.exception.CannotDeletePaymentMethodException;
import com.affiliate.rentals.gydi.subscriptions.domain.exception.PaymentMethodNotFoundException;
import com.affiliate.rentals.gydi.subscriptions.domain.model.PaymentMethod;
import com.affiliate.rentals.gydi.subscriptions.domain.model.PaymentMethodType;
import com.affiliate.rentals.gydi.subscriptions.domain.ports.PaymentGatewayPort;
import com.affiliate.rentals.gydi.subscriptions.domain.ports.PaymentGatewayPort.PaymentMethodResult;
import com.affiliate.rentals.gydi.subscriptions.domain.ports.PaymentMethodRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DeletePaymentMethodUseCase}.
 *
 * <p>Business Rules Tested:</p>
 * <ul>
 * <li>Users must keep at least one active payment method (any plan)</li>
 * <li>Default payment method cannot be deleted while other methods exist</li>
 * <li>Only the payment method owner can delete it</li>
 * <li>Payment method is soft-deleted (INACTIVE) and detached from Stripe</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DeletePaymentMethodUseCase Tests")
class DeletePaymentMethodUseCaseTest {

    @Mock
    private PaymentMethodRepositoryPort paymentMethodRepository;

    @Mock
    private PaymentGatewayPort paymentGateway;

    private DeletePaymentMethodUseCase deletePaymentMethodUseCase;

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long PAYMENT_METHOD_ID = 100L;
    private static final String GATEWAY_TOKEN = "pm_1234567890";

    private PaymentMethod testPaymentMethod;
    private PaymentMethod defaultPaymentMethod;
    private PaymentMethodResult mockPaymentMethodResult;

    @BeforeEach
    void setUp() {
        deletePaymentMethodUseCase = new DeletePaymentMethodUseCase(
                paymentMethodRepository,
                Optional.of(paymentGateway));

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

        mockPaymentMethodResult = new PaymentMethodResult(
                GATEWAY_TOKEN, "card", "visa", "4242", 12, 2028);
    }

    @Test
    @DisplayName("Should soft delete and detach from Stripe when all validations pass")
    void shouldDeletePaymentMethodSuccessfully() {
        when(paymentMethodRepository.findById(PAYMENT_METHOD_ID)).thenReturn(Optional.of(testPaymentMethod));
        when(paymentMethodRepository.countActiveByUserId(USER_ID)).thenReturn(2L);
        when(paymentMethodRepository.save(any(PaymentMethod.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentGateway.detachPaymentMethod(GATEWAY_TOKEN)).thenReturn(mockPaymentMethodResult);

        assertThatCode(() -> deletePaymentMethodUseCase.execute(PAYMENT_METHOD_ID, USER_ID))
                .doesNotThrowAnyException();

        verify(paymentMethodRepository).findById(PAYMENT_METHOD_ID);
        verify(paymentMethodRepository).countActiveByUserId(USER_ID);
        verify(paymentMethodRepository).save(any(PaymentMethod.class));
        verify(paymentGateway).detachPaymentMethod(GATEWAY_TOKEN);
        verify(paymentMethodRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Should throw PaymentMethodNotFoundException when payment method does not exist")
    void shouldThrowExceptionWhenPaymentMethodNotFound() {
        when(paymentMethodRepository.findById(PAYMENT_METHOD_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deletePaymentMethodUseCase.execute(PAYMENT_METHOD_ID, USER_ID))
                .isInstanceOf(PaymentMethodNotFoundException.class)
                .hasMessageContaining(String.valueOf(PAYMENT_METHOD_ID));

        verify(paymentMethodRepository).findById(PAYMENT_METHOD_ID);
        verify(paymentMethodRepository, never()).save(any());
        verifyNoInteractions(paymentGateway);
    }

    @Test
    @DisplayName("Should throw CannotDeletePaymentMethodException when payment method belongs to another user")
    void shouldThrowExceptionWhenPaymentMethodBelongsToOtherUser() {
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

        assertThatThrownBy(() -> deletePaymentMethodUseCase.execute(PAYMENT_METHOD_ID, USER_ID))
                .isInstanceOf(CannotDeletePaymentMethodException.class)
                .hasMessageContaining("not found or does not belong to you");

        verify(paymentMethodRepository).findById(PAYMENT_METHOD_ID);
        verify(paymentMethodRepository, never()).save(any());
        verifyNoInteractions(paymentGateway);
    }

    @Test
    @DisplayName("Should throw CannotDeletePaymentMethodException when deleting the only active method")
    void shouldThrowExceptionWhenDeletingOnlyMethod() {
        when(paymentMethodRepository.findById(PAYMENT_METHOD_ID)).thenReturn(Optional.of(testPaymentMethod));
        when(paymentMethodRepository.countActiveByUserId(USER_ID)).thenReturn(1L);

        assertThatThrownBy(() -> deletePaymentMethodUseCase.execute(PAYMENT_METHOD_ID, USER_ID))
                .isInstanceOf(CannotDeletePaymentMethodException.class)
                .hasMessageContaining("only active payment method");

        verify(paymentMethodRepository).findById(PAYMENT_METHOD_ID);
        verify(paymentMethodRepository).countActiveByUserId(USER_ID);
        verify(paymentMethodRepository, never()).save(any());
        verifyNoInteractions(paymentGateway);
    }

    @Test
    @DisplayName("Should throw CannotDeletePaymentMethodException when deleting default method with other methods present")
    void shouldThrowExceptionWhenDeletingDefaultWithOtherMethods() {
        when(paymentMethodRepository.findById(PAYMENT_METHOD_ID)).thenReturn(Optional.of(defaultPaymentMethod));
        when(paymentMethodRepository.countActiveByUserId(USER_ID)).thenReturn(2L);

        assertThatThrownBy(() -> deletePaymentMethodUseCase.execute(PAYMENT_METHOD_ID, USER_ID))
                .isInstanceOf(CannotDeletePaymentMethodException.class)
                .hasMessageContaining("default payment method");

        verify(paymentMethodRepository).findById(PAYMENT_METHOD_ID);
        verify(paymentMethodRepository).countActiveByUserId(USER_ID);
        verify(paymentMethodRepository, never()).save(any());
        verifyNoInteractions(paymentGateway);
    }

    @Test
    @DisplayName("Should proceed with soft delete even when Stripe detachment fails")
    void shouldProceedWhenStripeFails() {
        when(paymentMethodRepository.findById(PAYMENT_METHOD_ID)).thenReturn(Optional.of(testPaymentMethod));
        when(paymentMethodRepository.countActiveByUserId(USER_ID)).thenReturn(2L);
        when(paymentMethodRepository.save(any(PaymentMethod.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentGateway.detachPaymentMethod(GATEWAY_TOKEN))
                .thenThrow(new RuntimeException("Stripe unavailable"));

        assertThatCode(() -> deletePaymentMethodUseCase.execute(PAYMENT_METHOD_ID, USER_ID))
                .doesNotThrowAnyException();

        verify(paymentMethodRepository).save(any(PaymentMethod.class));
        verify(paymentGateway).detachPaymentMethod(GATEWAY_TOKEN);
    }

    @Test
    @DisplayName("Should skip Stripe detachment when payment gateway is not available")
    void shouldSkipStripeWhenGatewayUnavailable() {
        DeletePaymentMethodUseCase useCaseWithoutGateway = new DeletePaymentMethodUseCase(
                paymentMethodRepository,
                Optional.empty());

        when(paymentMethodRepository.findById(PAYMENT_METHOD_ID)).thenReturn(Optional.of(testPaymentMethod));
        when(paymentMethodRepository.countActiveByUserId(USER_ID)).thenReturn(2L);
        when(paymentMethodRepository.save(any(PaymentMethod.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() -> useCaseWithoutGateway.execute(PAYMENT_METHOD_ID, USER_ID))
                .doesNotThrowAnyException();

        verify(paymentMethodRepository).save(any(PaymentMethod.class));
        verifyNoInteractions(paymentGateway);
    }

    @Test
    @DisplayName("Should call repository methods exactly once for successful soft deletion")
    void shouldCallRepositoryMethodsExactlyOnce() {
        when(paymentMethodRepository.findById(PAYMENT_METHOD_ID)).thenReturn(Optional.of(testPaymentMethod));
        when(paymentMethodRepository.countActiveByUserId(USER_ID)).thenReturn(2L);
        when(paymentMethodRepository.save(any(PaymentMethod.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentGateway.detachPaymentMethod(GATEWAY_TOKEN)).thenReturn(mockPaymentMethodResult);

        deletePaymentMethodUseCase.execute(PAYMENT_METHOD_ID, USER_ID);

        verify(paymentMethodRepository, times(1)).findById(PAYMENT_METHOD_ID);
        verify(paymentMethodRepository, times(1)).countActiveByUserId(USER_ID);
        verify(paymentMethodRepository, times(1)).save(any(PaymentMethod.class));
        verify(paymentGateway, times(1)).detachPaymentMethod(GATEWAY_TOKEN);
        verify(paymentMethodRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Should handle null payment method ID gracefully")
    void shouldHandleNullPaymentMethodIdGracefully() {
        when(paymentMethodRepository.findById(null)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deletePaymentMethodUseCase.execute(null, USER_ID))
                .isInstanceOf(PaymentMethodNotFoundException.class);

        verify(paymentMethodRepository).findById(null);
        verify(paymentMethodRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should handle null user ID gracefully")
    void shouldHandleNullUserIdGracefully() {
        when(paymentMethodRepository.findById(PAYMENT_METHOD_ID)).thenReturn(Optional.of(testPaymentMethod));

        assertThatThrownBy(() -> deletePaymentMethodUseCase.execute(PAYMENT_METHOD_ID, null))
                .isInstanceOf(CannotDeletePaymentMethodException.class)
                .hasMessageContaining("not found or does not belong to you");

        verify(paymentMethodRepository).findById(PAYMENT_METHOD_ID);
        verify(paymentMethodRepository, never()).save(any());
    }
}
