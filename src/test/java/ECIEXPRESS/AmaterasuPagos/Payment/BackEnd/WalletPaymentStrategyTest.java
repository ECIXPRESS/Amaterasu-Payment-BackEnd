package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Web.Dto.PaymentRequests.CreatePaymentRequest;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Web.Dto.PaymentResponses.CreatePaymentResponse;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Services.Strategy.WalletPaymentStrategy;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Payment;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Wallet;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.WalletPayment;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.PaymentMethodType;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.PaymentStatus;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Ports.PromotionProvider;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Ports.ReceiptProvider;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Ports.WalletProvider;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Promotion.Dto.PromotionResponses.PromotionResponse;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Receipt.Dto.ReceiptResponses.CreateReceiptResponse;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Wallet.Dto.WalletResponses.CreateWalletResponse;
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
class WalletPaymentStrategyTest {

    @Mock PromotionProvider promotionProvider;
    @Mock WalletProvider walletProvider;
    @Mock ReceiptProvider receiptProvider;

    private WalletPaymentStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new WalletPaymentStrategy(promotionProvider, walletProvider, receiptProvider);
    }

    @Test
    void createPayment_Success() {
        var wallet = new Wallet();
        wallet.setPaymentMethodType(PaymentMethodType.WALLET);
        var request = new CreatePaymentRequest("ORDER-2", "CLIENT-2", "STORE-2", 80000.0, wallet, null);

        var promo = new PromotionResponse(70000.0, List.of("PROMO-10"));
        when(promotionProvider.applyPromotions("ORDER-2")).thenReturn(promo);

        when(walletProvider.processPayment(any(CreatePaymentRequest.class)))
                .thenReturn(new CreateWalletResponse(PaymentStatus.COMPLETED));

        var receipt = new CreateReceiptResponse("R-2", "ORDER-2", "STORE-2", 70000.0, PaymentStatus.COMPLETED);
        when(receiptProvider.createReceipt(any(Payment.class))).thenReturn(receipt);

        CreatePaymentResponse response = strategy.createPayment(request);

        assertNotNull(response);
        assertEquals("R-2", response.receiptId());
        assertEquals("ORDER-2", response.orderId());
        assertEquals("STORE-2", response.storeId());
        assertEquals(70000.0, response.finalAmount());
        assertEquals(PaymentStatus.COMPLETED, response.paymentStatus());

        ArgumentCaptor<CreatePaymentRequest> reqCaptor = ArgumentCaptor.forClass(CreatePaymentRequest.class);
        verify(walletProvider, times(1)).processPayment(reqCaptor.capture());
        assertEquals(70000.0, reqCaptor.getValue().originalAmount());

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(receiptProvider, times(1)).createReceipt(paymentCaptor.capture());
        Payment captured = paymentCaptor.getValue();
        assertTrue(captured instanceof WalletPayment);
        assertEquals("ORDER-2", captured.getOrderId());
        assertEquals("CLIENT-2", captured.getClientId());
        assertEquals("STORE-2", captured.getStoreId());
        assertEquals(70000.0, captured.getFinalAmount());
        assertEquals(PaymentStatus.COMPLETED, captured.getPaymentStatus());
        assertNotNull(captured.getTimeStamps());
        assertNotNull(captured.getTimeStamps().getCreatedAt());
        assertNotNull(captured.getTimeStamps().getPaymentProcessedAt());
    }

    @Test
    void createPayment_WhenWalletProviderFails_Propagates() {
        var wallet = new Wallet();
        wallet.setPaymentMethodType(PaymentMethodType.WALLET);
        var request = new CreatePaymentRequest("ORDER-ERR", "CLIENT-X", "STORE-Y", 1000.0, wallet, null);

        when(promotionProvider.applyPromotions(anyString()))
                .thenReturn(new PromotionResponse(900.0, List.of("P")));
        when(walletProvider.processPayment(any(CreatePaymentRequest.class)))
                .thenThrow(new RuntimeException("wallet error"));

        assertThrows(RuntimeException.class, () -> strategy.createPayment(request));
        verify(receiptProvider, never()).createReceipt(any());
    }
}