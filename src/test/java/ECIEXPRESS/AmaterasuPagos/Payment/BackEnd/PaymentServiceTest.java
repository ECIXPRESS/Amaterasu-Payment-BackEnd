package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Web.Dto.PaymentRequests.CreatePaymentRequest;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Web.Dto.PaymentResponses.CreatePaymentResponse;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Services.PaymentService;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Services.Strategy.PaymentStrategy;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.*;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.BankPaymentType;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.PaymentMethodType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentStrategy bankPaymentStrategy;

    @Mock
    private PaymentStrategy cashPaymentStrategy;

    @Mock
    private PaymentStrategy walletPaymentStrategy;

    private PaymentService paymentService;

    private CreatePaymentRequest bankPaymentRequest;
    private CreatePaymentRequest cashPaymentRequest;
    private CreatePaymentRequest walletPaymentRequest;

    @BeforeEach
    void setUp() {

        paymentService = new PaymentService();
        Map<PaymentMethodType, PaymentStrategy> mockStrategyMap = Map.of(
                PaymentMethodType.BANK, bankPaymentStrategy,
                PaymentMethodType.WALLET, walletPaymentStrategy,
                PaymentMethodType.CASH, cashPaymentStrategy
        );
        try {
            Field field = PaymentService.class.getDeclaredField("strategyMap");
            field.setAccessible(true);
            field.set(paymentService, mockStrategyMap);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // Crear requests de prueba
        BankDetails bankDetails = new BankDetails();
        bankDetails.setBankName("Bancolombia");
        bankDetails.setBankPaymentType(BankPaymentType.CREDIT_CARD);

        PaymentMethod bankPaymentMethod = new Bank() {
            @Override
            public PaymentMethod createPaymentMethod() {
                return this;
            }
        };
        bankPaymentMethod.setPaymentMethodType(PaymentMethodType.BANK);

        PaymentMethod cashPaymentMethod = new Cash() {
            @Override
            public PaymentMethod createPaymentMethod() {
                return this;
            }
        };
        cashPaymentMethod.setPaymentMethodType(PaymentMethodType.CASH);

        PaymentMethod walletPaymentMethod = new Wallet() {
            @Override
            public PaymentMethod createPaymentMethod() {
                return this;
            }
        };
        walletPaymentMethod.setPaymentMethodType(PaymentMethodType.WALLET);

        bankPaymentRequest = new CreatePaymentRequest(
                "ORDER-123", "CLIENT-456", "STORE-789",
                100000.0, bankPaymentMethod, bankDetails
        );

        cashPaymentRequest = new CreatePaymentRequest(
                "ORDER-124", "CLIENT-457", "STORE-790",
                50000.0, cashPaymentMethod, null
        );

        walletPaymentRequest = new CreatePaymentRequest(
                "ORDER-125", "CLIENT-458", "STORE-791",
                75000.0, walletPaymentMethod, null
        );
    }

    @Test
    void createPayment_WithBankPayment_ShouldCallBankStrategy() {
        // Given
        CreatePaymentResponse expectedResponse = new CreatePaymentResponse(
                "RECEIPT-123", "ORDER-123", "STORE-789", 100000.0, null
        );
        when(bankPaymentStrategy.createPayment(any())).thenReturn(expectedResponse);

        // When
        CreatePaymentResponse result = paymentService.createPayment(bankPaymentRequest);

        // Then
        assertNotNull(result);
        verify(bankPaymentStrategy, times(1)).createPayment(bankPaymentRequest);
        verify(cashPaymentStrategy, never()).createPayment(any());
        verify(walletPaymentStrategy, never()).createPayment(any());
    }

    @Test
    void createPayment_WithCashPayment_ShouldCallCashStrategy() {
        // Given
        CreatePaymentResponse expectedResponse = new CreatePaymentResponse(
                "RECEIPT-124", "ORDER-124", "STORE-790", 50000.0, null
        );
        when(cashPaymentStrategy.createPayment(any())).thenReturn(expectedResponse);

        // When
        CreatePaymentResponse result = paymentService.createPayment(cashPaymentRequest);

        // Then
        assertNotNull(result);
        verify(cashPaymentStrategy, times(1)).createPayment(cashPaymentRequest);
        verify(bankPaymentStrategy, never()).createPayment(any());
        verify(walletPaymentStrategy, never()).createPayment(any());
    }

    @Test
    void createPayment_WithWalletPayment_ShouldCallWalletStrategy() {
        // Given
        CreatePaymentResponse expectedResponse = new CreatePaymentResponse(
                "RECEIPT-125", "ORDER-125", "STORE-791", 75000.0, null
        );
        when(walletPaymentStrategy.createPayment(any())).thenReturn(expectedResponse);

        // When
        CreatePaymentResponse result = paymentService.createPayment(walletPaymentRequest);

        // Then
        assertNotNull(result);
        verify(walletPaymentStrategy, times(1)).createPayment(walletPaymentRequest);
        verify(bankPaymentStrategy, never()).createPayment(any());
        verify(cashPaymentStrategy, never()).createPayment(any());
    }
}