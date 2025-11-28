package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Web.Dto.PaymentRequests.CreatePaymentRequest;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Bank;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.BankDetails;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.BankAccountType;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.BankPaymentType;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.BankResponseCode;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.PaymentMethodType;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.GatewayResponse;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.BankGateway.PayuBankGatewayAdapter;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;

class PayuBankGatewayAdapterTest {

    private MockWebServer mockWebServer;
    private PayuBankGatewayAdapter payuBankGatewayAdapter;

    @BeforeEach
    void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        payuBankGatewayAdapter = new PayuBankGatewayAdapter(new RestTemplate());
        setField(payuBankGatewayAdapter, "baseUrl", mockWebServer.url("/api/payu").toString());
        setField(payuBankGatewayAdapter, "apiLogin", "login");
        setField(payuBankGatewayAdapter, "apiKey", "key");
        setField(payuBankGatewayAdapter, "merchantId", "merchant");
        setField(payuBankGatewayAdapter, "accountId", "account");
        setField(payuBankGatewayAdapter, "currency", "COP");
        setField(payuBankGatewayAdapter, "testMode", true);
    }

    @AfterEach
    void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    void processPayment_WithApprovedResponse_ShouldMapToSuccess() {
        var bankMethod = new Bank();
        bankMethod.setPaymentMethodType(PaymentMethodType.BANK);
        bankMethod.setBankPaymentType(BankPaymentType.CREDIT_CARD);
        bankMethod.setBankAccountType(BankAccountType.CHECKING_ACCOUNT);

        var bankDetails = new BankDetails();
        bankDetails.setBankName("Bancolombia");
        bankDetails.setBankPaymentType(BankPaymentType.CREDIT_CARD);
        bankDetails.setBankAccountType(BankAccountType.CHECKING_ACCOUNT);
        bankDetails.setAccountNumber("4111111111111111");
        bankDetails.setExpiryDate("12/25");
        bankDetails.setCvv("123");
        bankDetails.setCardHolderName("John Doe");

        var request = new CreatePaymentRequest(
                "ORDER-400", "CLIENT-999", "STORE-777", 100000.0, bankMethod, bankDetails
        );

        var jsonResponse = """
        {
          "code": "SUCCESS",
          "transactionResponse": {
            "transactionId": "TX123",
            "state": "APPROVED",
            "authorizationCode": "AUTH999",
            "responseCode": "APPROVED",
            "responseMessage": "APPROVED OK"
          }
        }
        """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(jsonResponse));

        GatewayResponse response = payuBankGatewayAdapter.processPayment(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("TX123", response.getBankReceiptNumber());
        assertEquals("AUTH999", response.getAuthorizationNumber());
        assertEquals("APPROVED OK", response.getGatewayMessage());
        assertEquals("APPROVED", response.getResponseCode());
        assertEquals(BankResponseCode.APPROVED, response.getBankResponseCode());
        assertEquals(100000.0, response.getProcessedAmount());
        assertEquals("COP", response.getCurrency());
    }

    @Test
    void processPayment_WithServerError_ShouldReturnErrorResponse() {
        var bankMethod = new Bank();
        bankMethod.setPaymentMethodType(PaymentMethodType.BANK);
        bankMethod.setBankPaymentType(BankPaymentType.CREDIT_CARD);
        bankMethod.setBankAccountType(BankAccountType.SAVINGS_ACCOUNT);

        var bankDetails = new BankDetails();
        bankDetails.setBankName("Bancolombia");
        bankDetails.setBankPaymentType(BankPaymentType.CREDIT_CARD);
        bankDetails.setBankAccountType(BankAccountType.SAVINGS_ACCOUNT);
        bankDetails.setAccountNumber("4111111111111111");
        bankDetails.setExpiryDate("01/29");
        bankDetails.setCvv("999");
        bankDetails.setCardHolderName("Jane Doe");

        var request = new CreatePaymentRequest(
                "ORDER-401", "CLIENT-888", "STORE-666", 120000.0, bankMethod, bankDetails
        );

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("error"));

        GatewayResponse response = payuBankGatewayAdapter.processPayment(request);

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("PAYU_PROCESSING_ERROR", response.getResponseCode());
        assertEquals(BankResponseCode.BANK_UNAVAILABLE, response.getBankResponseCode());
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
