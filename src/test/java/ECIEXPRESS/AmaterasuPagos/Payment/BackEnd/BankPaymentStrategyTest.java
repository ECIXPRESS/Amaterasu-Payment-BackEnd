package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.*;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Web.Dto.PaymentRequests.CreatePaymentRequest;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Web.Dto.PaymentResponses.CreatePaymentResponse;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Services.Strategy.BankPaymentStrategy;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Services.ValidationService;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.*;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Ports.BankGatewayProvider;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Ports.PromotionProvider;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Ports.ReceiptProvider;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Promotion.Dto.PromotionResponses.PromotionResponse;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Receipt.Dto.ReceiptResponses.CreateReceiptResponse;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Exception.BankValidationException;
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
class BankPaymentStrategyTest {

    @Mock PromotionProvider promotionProvider;
    @Mock BankGatewayProvider bankGatewayProvider;
    @Mock ValidationService validationService;
    @Mock ReceiptProvider receiptProvider;

    private BankPaymentStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new BankPaymentStrategy(promotionProvider, bankGatewayProvider, validationService, receiptProvider);
    }

    @Test
    void createPayment_Success() {
        var bankMethod = new Bank();
        bankMethod.setPaymentMethodType(PaymentMethodType.BANK);
        bankMethod.setBankPaymentType(BankPaymentType.CREDIT_CARD);
        bankMethod.setBankAccountType(BankAccountType.CHECKING_ACCOUNT);

        var details = new BankDetails();
        details.setBankName("Bancolombia");
        details.setBankPaymentType(BankPaymentType.CREDIT_CARD);
        details.setBankAccountType(BankAccountType.CHECKING_ACCOUNT);
        details.setAccountNumber("4111111111111111");
        details.setExpiryDate("12/30");
        details.setCvv("123");
        details.setCardHolderName("John Doe");

        var request = new CreatePaymentRequest("ORDER-3", "CLIENT-3", "STORE-3", 100000.0, bankMethod, details);

        BankValidationResult validation = new BankValidationResult();
        when(validationService.createValidation(details)).thenReturn(validation);

        var promo = new PromotionResponse(90000.0, List.of("PROMO-10"));
        when(promotionProvider.applyPromotions("ORDER-3")).thenReturn(promo);

        GatewayResponse gatewayResponse = new GatewayResponse(
                true, "BRN-123", "AUTH-456", "APPROVED", "APPROVED", BankResponseCode.APPROVED, 90000.0, "COP");
        when(bankGatewayProvider.processPayment(any(CreatePaymentRequest.class))).thenReturn(gatewayResponse);

        var receipt = new CreateReceiptResponse("R-3", "ORDER-3", "STORE-3", 90000.0, ReceiptStatus.PAYED);
        when(receiptProvider.createReceipt(any(Payment.class))).thenReturn(receipt);

        CreatePaymentResponse response = strategy.createPayment(request);

        assertNotNull(response);
        assertEquals("R-3", response.receiptId());
        assertEquals("ORDER-3", response.orderId());
        assertEquals("STORE-3", response.storeId());
        assertEquals(90000.0, response.finalAmount());
        assertEquals(ReceiptStatus.PAYED, response.receiptStatus());

        ArgumentCaptor<CreatePaymentRequest> reqCaptor = ArgumentCaptor.forClass(CreatePaymentRequest.class);
        verify(bankGatewayProvider, times(1)).processPayment(reqCaptor.capture());
        assertEquals(90000.0, reqCaptor.getValue().originalAmount());

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(receiptProvider, times(1)).createReceipt(paymentCaptor.capture());
        Payment captured = paymentCaptor.getValue();
        assertTrue(captured instanceof BankPayment);
        BankPayment bankPayment = (BankPayment) captured;
        assertSame(validation, bankPayment.getBankValidationResult());
        assertSame(gatewayResponse, bankPayment.getGatewayResponse());
        assertEquals("ORDER-3", bankPayment.getOrderId());
        assertEquals("CLIENT-3", bankPayment.getClientId());
        assertEquals("STORE-3", bankPayment.getStoreId());
        assertEquals(90000.0, bankPayment.getFinalAmount());
        assertEquals(PaymentStatus.COMPLETED, bankPayment.getPaymentStatus());
        assertNotNull(bankPayment.getTimeStamps());
        assertNotNull(bankPayment.getTimeStamps().getCreatedAt());
        assertNotNull(bankPayment.getTimeStamps().getPaymentProcessedAt());
    }

    @Test
    void createPayment_WhenValidationFails_Propagates() {
        var bankMethod = new Bank();
        bankMethod.setPaymentMethodType(PaymentMethodType.BANK);
        bankMethod.setBankPaymentType(BankPaymentType.CREDIT_CARD);
        bankMethod.setBankAccountType(BankAccountType.CHECKING_ACCOUNT);

        var details = new BankDetails();
        details.setBankName("Bancolombia");
        details.setBankPaymentType(BankPaymentType.CREDIT_CARD);
        details.setBankAccountType(BankAccountType.CHECKING_ACCOUNT);
        details.setAccountNumber("0000");
        details.setExpiryDate("01/20");
        details.setCvv("12");
        details.setCardHolderName("X");

        var request = new CreatePaymentRequest("ORDER-ERR", "CLIENT-X", "STORE-Y", 1000.0, bankMethod, details);

        when(validationService.createValidation(details))
                .thenThrow(new BankValidationException(new BankValidationResult(), "validation failed"));

        assertThrows(BankValidationException.class, () -> strategy.createPayment(request));
        verify(receiptProvider, never()).createReceipt(any());
        verify(bankGatewayProvider, never()).processPayment(any());
    }
}