package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Cash;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.CashPayment;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.PaymentMethodType;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.PaymentStatus;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.TimeStamps;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Receipt.ReceiptProviderAdapter;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Receipt.Dto.ReceiptResponses.CreateReceiptResponse;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReceiptProviderAdapterTest {

    private MockWebServer mockWebServer;
    private ReceiptProviderAdapter receiptProviderAdapter;

    @BeforeEach
    void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        receiptProviderAdapter = new ReceiptProviderAdapter(new RestTemplate());
        setField(receiptProviderAdapter, "baseUrl", mockWebServer.url("/").toString());
        setField(receiptProviderAdapter, "basePath", "/api/v1/receipts");
    }

    @AfterEach
    void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    void createReceipt_WithSuccessfulResponse_ShouldReturnReceiptResponse() {
        var cash = new Cash();
        cash.setPaymentMethodType(PaymentMethodType.CASH);

        var payment = new CashPayment();
        payment.setOrderId("ORDER-300");
        payment.setClientId("CLIENT-123");
        payment.setStoreId("STORE-789");
        payment.setFinalAmount(95000.0);
        payment.setPaymentMethod(cash);
        payment.setPaymentStatus(PaymentStatus.COMPLETED);
        payment.setTimeStamps(new TimeStamps("now", "later"));
        payment.setAppliedPromotions(List.of("PROMO-10"));

        var jsonResponse = """
        {
          "receiptId":"RECEIPT-300",
          "orderId":"ORDER-300",
          "storeId":"STORE-789",
          "finalAmount":95000.0,
          "paymentStatus":"COMPLETED"
        }
        """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(jsonResponse));

        CreateReceiptResponse response = receiptProviderAdapter.createReceipt(payment);

        assertNotNull(response);
        assertEquals("RECEIPT-300", response.receiptId());
        assertEquals("ORDER-300", response.orderId());
        assertEquals("STORE-789", response.storeId());
        assertEquals(95000.0, response.finalAmount());
        assertEquals(PaymentStatus.COMPLETED, response.paymentStatus());
    }

    @Test
    void createReceipt_WithServerError_ShouldReturnFallbackResponse() {
        var cash = new Cash();
        cash.setPaymentMethodType(PaymentMethodType.CASH);

        var payment = new CashPayment();
        payment.setOrderId("ORDER-301");
        payment.setClientId("CLIENT-456");
        payment.setStoreId("STORE-012");
        payment.setFinalAmount(100000.0);
        payment.setPaymentMethod(cash);
        payment.setPaymentStatus(PaymentStatus.PROCESSING);

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("error"));

        CreateReceiptResponse response = receiptProviderAdapter.createReceipt(payment);

        assertNotNull(response);
        assertNull(response.receiptId());
        assertNull(response.orderId());
        assertNull(response.storeId());
        assertEquals(0.0, response.finalAmount());
        assertNull(response.paymentStatus());
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