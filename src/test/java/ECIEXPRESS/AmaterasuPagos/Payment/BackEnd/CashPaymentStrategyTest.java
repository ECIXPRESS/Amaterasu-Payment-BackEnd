package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto.PaymentRequests.CreatePaymentRequest;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto.PaymentResponses.CreatePaymentResponse;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Services.Strategy.CashPaymentStrategy;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Cash;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.CashPayment;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Payment;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.PaymentMethodType;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.PaymentStatus;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Ports.PromotionProvider;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Ports.ReceiptProvider;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Promotion.Dto.PromotionResponses.PromotionResponse;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Receipt.Dto.ReceiptResponses.CreateReceiptResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CashPaymentStrategyTest {

    @Mock PromotionProvider promotionProvider;
    @Mock ReceiptProvider receiptProvider;

    private CashPaymentStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new CashPaymentStrategy(promotionProvider, receiptProvider);
    }

    @Test
    void createPayment_Success() {
        var cash = new Cash();
        cash.setPaymentMethodType(PaymentMethodType.CASH);
        var request = new CreatePaymentRequest("ORDER-1", "CLIENT-1", "STORE-1", 100000.0, cash, null);

        var promo = new PromotionResponse(95000.0, List.of("PROMO-5", "PROMO-10"));
        when(promotionProvider.applyPromotions("ORDER-1")).thenReturn(promo);

        var receipt = new CreateReceiptResponse("R-1", "ORDER-1", "STORE-1", 95000.0, PaymentStatus.PENDING);
        when(receiptProvider.createReceipt(any(Payment.class))).thenReturn(receipt);

        CreatePaymentResponse response = strategy.createPayment(request);

        assertNotNull(response);
        assertEquals("R-1", response.receiptId());
        assertEquals("ORDER-1", response.orderId());
        assertEquals("STORE-1", response.storeId());
        assertEquals(95000.0, response.finalAmount());
        assertEquals(PaymentStatus.PENDING, response.paymentStatus());

        verify(promotionProvider, times(1)).applyPromotions("ORDER-1");

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(receiptProvider, times(1)).createReceipt(paymentCaptor.capture());
        Payment captured = paymentCaptor.getValue();
        assertTrue(captured instanceof CashPayment);
        assertEquals("ORDER-1", captured.getOrderId());
        assertEquals("CLIENT-1", captured.getClientId());
        assertEquals("STORE-1", captured.getStoreId());
        assertEquals(95000.0, captured.getFinalAmount());
        assertEquals(PaymentStatus.PENDING, captured.getPaymentStatus());
        assertNotNull(captured.getTimeStamps());
        assertNotNull(captured.getTimeStamps().getCreatedAt());
    }

    @Test
    void createPayment_WhenPromotionFails_Propagates() {
        var cash = new Cash();
        cash.setPaymentMethodType(PaymentMethodType.CASH);
        var request = new CreatePaymentRequest("ORDER-ERR", "CLIENT-X", "STORE-Y", 1000.0, cash, null);

        when(promotionProvider.applyPromotions(anyString())).thenThrow(new RuntimeException("promo error"));

        assertThrows(RuntimeException.class, () -> strategy.createPayment(request));
        verify(receiptProvider, never()).createReceipt(any());
    }
}