package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto.PaymentRequests.CreatePaymentRequest;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto.PaymentResponses.CreatePaymentResponse;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Services.PaymentService;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.BankDetails;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Cash;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.BankAccountType;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.BankPaymentType;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.PaymentMethodType;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Web.PaymentController;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
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
    private MockMvc mockMvc;

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
                "RECEIPT-123", "ORDER-123", "STORE-789", 95000.0, null
        );

        when(paymentService.createPayment(any(CreatePaymentRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/payments/ProcessPayment")
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
        mockMvc.perform(post("/api/v1/payments/ProcessPayment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isCreated());
    }
}