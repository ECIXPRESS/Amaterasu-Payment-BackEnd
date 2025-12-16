package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.ReceiptStatus;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Web.Dto.PaymentRequests.CreatePaymentRequest;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Web.Dto.PaymentResponses.CreatePaymentResponse;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Services.PaymentService;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.BankDetails;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Cash;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.BankAccountType;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.BankPaymentType;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.PaymentMethodType;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Web.PaymentController;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @Test
    void createPayment_WithValidRequest_ShouldReturnCreated() throws Exception {
        // Given
        BankDetails bankDetails = new BankDetails();
        bankDetails.setBankName("Bancolombia");
        bankDetails.setBankPaymentType(BankPaymentType.CREDIT_CARD);
        bankDetails.setBankAccountType(BankAccountType.CHECKING_ACCOUNT);

        Cash paymentMethod = new Cash();
        paymentMethod.setPaymentMethodType(PaymentMethodType.CASH);

        CreatePaymentRequest request = new CreatePaymentRequest(
                "ORDER-123", "CLIENT-456", "STORE-789",
                100000.0, paymentMethod, bankDetails
        );

        CreatePaymentResponse response = new CreatePaymentResponse(
                "RECEIPT-123", "ORDER-123", "STORE-789", 95000.0, null, "QR_CODE"
        );

        when(paymentService.createPayment(any(CreatePaymentRequest.class))).thenReturn(response);

        // When & Then
        mvc.perform(post("/api/v1/payments/ProcessPayment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.receiptId").value("RECEIPT-123"))
                .andExpect(jsonPath("$.orderId").value("ORDER-123"))
                .andExpect(jsonPath("$.storeId").value("STORE-789"))
                .andExpect(jsonPath("$.finalAmount").value(95000.0));
    }

    @Test
    void createPayment_WithInvalidRequest_ShouldReturnCreated() throws Exception {
        // Given - Request sin orderId (inválido)
        CreatePaymentRequest invalidRequest = new CreatePaymentRequest(
                null, "CLIENT-456", "STORE-789", 100000.0, null, null
        );

        // When & Then
        mvc.perform(post("/api/v1/payments/ProcessPayment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isCreated());
    }

    @Test
    void processPayment_bank_returns201() throws Exception {
        var response = new CreatePaymentResponse(
                "receipt-1", "order-1001", "store-456", 65000.0, ReceiptStatus.PAYED, "qr"
        );

        Mockito.when(paymentService.createPayment(Mockito.any(CreatePaymentRequest.class)))
                .thenReturn(response);

        String body = """
    {
      "orderId":"order-1001",
      "clientId":"client-123",
      "storeId":"store-456",
      "originalAmount":65000,
      "paymentMethod":{
        "type":"BANK",
        "paymentMethodType":"BANK",
        "bankPaymentType":"CREDIT_CARD",
        "bankAccountType":"SAVINGS_ACCOUNT",
        "bankName":"PAYU_SANDBOX"
      },
      "bankDetails":{
        "bankName":"PAYU_SANDBOX",
        "bankPaymentType":"CREDIT_CARD",
        "bankAccountType":"SAVINGS_ACCOUNT",
        "accountNumber":"4111111111111111",
        "expiryDate":"05/30",
        "cvv":"777",
        "cardHolderName":"APPROVED",
        "validationStatus":"PENDING_VALIDATION"
      }
    }
    """;

        mvc.perform(post("/api/v1/payments/ProcessPayment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value("order-1001"))
                .andExpect(jsonPath("$.receiptStatus").value("PAYED"));
    }
}