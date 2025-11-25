package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto.PaymentDto;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Web.Dto.PaymentRequests.CreatePaymentRequest;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Web.Dto.PaymentResponses.CreatePaymentResponse;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Mappers.ApplicationMapper;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.BankDetails;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Cash;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.BankAccountType;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.BankPaymentType;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.PaymentMethodType;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.PaymentStatus;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.PaymentMethod;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.TimeStamps;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Promotion.Dto.PromotionResponses.PromotionResponse;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Receipt.Dto.ReceiptResponses.CreateReceiptResponse;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationMapperTest {

    @Test
    void createCashPaymentDto_WithValidData_ShouldReturnCorrectPaymentDto() {
        // Given
        PaymentMethod paymentMethod = createPaymentMethod(PaymentMethodType.CASH);
        BankDetails bankDetails = createBankDetails();

        CreatePaymentRequest request = new CreatePaymentRequest(
                "ORDER-123", "CLIENT-456", "STORE-789",
                100000.0, paymentMethod, bankDetails
        );

        List<String> appliedPromotions = Arrays.asList("PROMO-10%", "WELCOME-5");
        PromotionResponse promotionResponse = new PromotionResponse(90000.0, appliedPromotions);

        TimeStamps timeStamps = new TimeStamps("2024-01-01T10:00:00", null);

        // When
        PaymentDto result = ApplicationMapper.createCashPaymentDto(request, promotionResponse, timeStamps);

        // Then
        assertNotNull(result);
        assertEquals("ORDER-123", result.orderId());
        assertEquals("CLIENT-456", result.clientId());
        assertEquals("STORE-789", result.storeId());
        assertEquals(100000.0, result.originalAmount());
        assertEquals(90000.0, result.finalAmount());
        assertEquals(PaymentStatus.PENDING, result.paymentStatus());
        assertEquals(timeStamps, result.timeStamps());
        assertEquals(appliedPromotions, result.appliedPromotions());
        assertEquals(paymentMethod, result.paymentMethod());
    }

    @Test
    void createBankPaymentDto_WithValidData_ShouldReturnCompletedStatus() {
        // Given
        PaymentMethod paymentMethod = createPaymentMethod(PaymentMethodType.BANK);
        BankDetails bankDetails = createBankDetails();

        CreatePaymentRequest request = new CreatePaymentRequest(
                "ORDER-123", "CLIENT-456", "STORE-789",
                100000.0, paymentMethod, bankDetails
        );

        List<String> appliedPromotions = Arrays.asList("PROMO-5%");
        PromotionResponse promotionResponse = new PromotionResponse(95000.0, appliedPromotions);

        TimeStamps timeStamps = new TimeStamps("2024-01-01T10:00:00", "2024-01-01T10:05:00");

        // When
        PaymentDto result = ApplicationMapper.createBankPaymentDto(request, promotionResponse, timeStamps);

        // Then
        assertNotNull(result);
        assertEquals("ORDER-123", result.orderId());
        assertEquals("CLIENT-456", result.clientId());
        assertEquals("STORE-789", result.storeId());
        assertEquals(100000.0, result.originalAmount());
        assertEquals(95000.0, result.finalAmount());
        assertEquals(PaymentStatus.COMPLETED, result.paymentStatus());
        assertEquals(timeStamps, result.timeStamps());
        assertEquals(appliedPromotions, result.appliedPromotions());
        assertEquals(paymentMethod, result.paymentMethod());
    }

    @Test
    void updatePaymentRequest_WithPromotion_ShouldUpdateAmount() {
        // Given
        PaymentMethod paymentMethod = createPaymentMethod(PaymentMethodType.BANK);
        BankDetails bankDetails = createBankDetails();

        CreatePaymentRequest originalRequest = new CreatePaymentRequest(
                "ORDER-123", "CLIENT-456", "STORE-789",
                100000.0, paymentMethod, bankDetails
        );

        PromotionResponse promotionResponse = new PromotionResponse(85000.0,
                Arrays.asList("BIG-SALE"));

        // When
        CreatePaymentRequest updatedRequest = ApplicationMapper.updatePaymentRequest(
                originalRequest, promotionResponse);

        // Then
        assertNotNull(updatedRequest);
        assertEquals(85000.0, updatedRequest.originalAmount()); // Debería actualizar el monto
        assertEquals(originalRequest.orderId(), updatedRequest.orderId());
        assertEquals(originalRequest.clientId(), updatedRequest.clientId());
        assertEquals(originalRequest.storeId(), updatedRequest.storeId());
        assertEquals(originalRequest.paymentMethod(), updatedRequest.paymentMethod());
        assertEquals(originalRequest.bankDetails(), updatedRequest.bankDetails());
    }

    @Test
    void receiptResponseToPaymentResponse_ShouldMapAllFields() {
        // Given
        CreateReceiptResponse receiptResponse = new CreateReceiptResponse(
                "RECEIPT-123", "ORDER-123", "STORE-789", 95000.0, PaymentStatus.COMPLETED
        );

        // When
        CreatePaymentResponse result = ApplicationMapper.ReceiptResponseToPaymentResponse(receiptResponse);

        // Then
        assertNotNull(result);
        assertEquals("RECEIPT-123", result.receiptId());
        assertEquals("ORDER-123", result.orderId());
        assertEquals("STORE-789", result.storeId());
        assertEquals(95000.0, result.finalAmount());
        assertEquals(PaymentStatus.COMPLETED, result.paymentStatus());
    }

    @Test
    void createCashPaymentDto_WithNullPromotions_ShouldHandleGracefully() {
        // Given
        PaymentMethod paymentMethod = createPaymentMethod(PaymentMethodType.CASH);
        CreatePaymentRequest request = new CreatePaymentRequest(
                "ORDER-123", "CLIENT-456", "STORE-789",
                100000.0, paymentMethod, null
        );

        PromotionResponse promotionResponse = new PromotionResponse(90000.0, null);
        TimeStamps timeStamps = new TimeStamps("2024-01-01T10:00:00", null);

        // When
        PaymentDto result = ApplicationMapper.createCashPaymentDto(request, promotionResponse, timeStamps);

        // Then
        assertNotNull(result);
        assertEquals(90000.0, result.finalAmount());
        assertNull(result.appliedPromotions());
    }

    @Test
    void createBankPaymentDto_WithEmptyPromotions_ShouldHandleGracefully() {
        // Given
        PaymentMethod paymentMethod = createPaymentMethod(PaymentMethodType.BANK);
        CreatePaymentRequest request = new CreatePaymentRequest(
                "ORDER-123", "CLIENT-456", "STORE-789",
                100000.0, paymentMethod, null
        );

        PromotionResponse promotionResponse = new PromotionResponse(100000.0, Arrays.asList());
        TimeStamps timeStamps = new TimeStamps("2024-01-01T10:00:00", null);

        // When
        PaymentDto result = ApplicationMapper.createBankPaymentDto(request, promotionResponse, timeStamps);

        // Then
        assertNotNull(result);
        assertEquals(100000.0, result.finalAmount());
        assertTrue(result.appliedPromotions().isEmpty());
    }

    private PaymentMethod createPaymentMethod(PaymentMethodType type) {
        PaymentMethod paymentMethod = new Cash(); // or new Wallet() or new Bank()
        paymentMethod.setPaymentMethodType(type);
        return paymentMethod;
    }

    private BankDetails createBankDetails() {
        BankDetails bankDetails = new BankDetails();
        bankDetails.setBankName("Bancolombia");
        bankDetails.setBankPaymentType(BankPaymentType.CREDIT_CARD);
        bankDetails.setBankAccountType(BankAccountType.CHECKING_ACCOUNT);
        bankDetails.setAccountNumber("4111111111111111");
        bankDetails.setExpiryDate("12/25");
        bankDetails.setCvv("123");
        bankDetails.setCardHolderName("Juan Perez");
        return bankDetails;
    }
}