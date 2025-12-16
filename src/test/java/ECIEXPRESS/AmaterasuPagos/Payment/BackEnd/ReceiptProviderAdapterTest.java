package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Cash;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.CashPayment;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.PaymentMethodType;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.PaymentStatus;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Receipt.ReceiptProviderAdapter;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Receipt.Dto.ReceiptResponses.CreateReceiptResponse;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class ReceiptProviderAdapterCoverageTest {

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
    void createReceipt_WithNonSuccessStatus202_ShouldReturnFallbackResponse() throws Exception {
        var cash = new Cash();
        cash.setPaymentMethodType(PaymentMethodType.CASH);

        var payment = new CashPayment();
        payment.setOrderId("ORDER-310");
        payment.setClientId("CLIENT-310");
        payment.setStoreId("STORE-310");
        payment.setFinalAmount(12345.0);
        payment.setPaymentMethod(cash);
        payment.setPaymentStatus(PaymentStatus.PROCESSING);

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(202)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("{\"receiptId\":\"R-IGNORED\"}"));

        CreateReceiptResponse response = receiptProviderAdapter.createReceipt(payment);

        assertNotNull(response);
        assertNull(response.receiptId());
        assertEquals("ORDER-310", response.orderId());
        assertEquals("CLIENT-310", response.clientId());
        assertEquals("STORE-310", response.storeId());
        assertEquals(12345.0, response.finalAmount());
        assertNull(response.receiptStatus());

        RecordedRequest recorded = mockWebServer.takeRequest();
        assertEquals("/api/v1/receipts", recorded.getPath());
        assertTrue(recorded.getHeader(HttpHeaders.CONTENT_TYPE).contains(MediaType.APPLICATION_JSON_VALUE));
    }

    @Test
    void createReceipt_WithOk200ButEmptyBody_ShouldReturnFallbackResponse() {
        var cash = new Cash();
        cash.setPaymentMethodType(PaymentMethodType.CASH);

        var payment = new CashPayment();
        payment.setOrderId("ORDER-311");
        payment.setClientId("CLIENT-311");
        payment.setStoreId("STORE-311");
        payment.setFinalAmount(777.0);
        payment.setPaymentMethod(cash);
        payment.setPaymentStatus(PaymentStatus.PROCESSING);

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        CreateReceiptResponse response = receiptProviderAdapter.createReceipt(payment);

        assertNotNull(response);
        assertNull(response.receiptId());
        assertEquals("ORDER-311", response.orderId());
        assertEquals("CLIENT-311", response.clientId());
        assertEquals("STORE-311", response.storeId());
        assertEquals(777.0, response.finalAmount());
        assertNull(response.receiptStatus());
    }

    @Test
    void createReceipt_WithNotFound404_ShouldReturnFallbackResponse() {
        var cash = new Cash();
        cash.setPaymentMethodType(PaymentMethodType.CASH);

        var payment = new CashPayment();
        payment.setOrderId("ORDER-312");
        payment.setClientId("CLIENT-312");
        payment.setStoreId("STORE-312");
        payment.setFinalAmount(999.0);
        payment.setPaymentMethod(cash);
        payment.setPaymentStatus(PaymentStatus.PROCESSING);

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(404)
                .setBody("not found"));

        CreateReceiptResponse response = receiptProviderAdapter.createReceipt(payment);

        assertNotNull(response);
        assertNull(response.receiptId());
        assertEquals("ORDER-312", response.orderId());
        assertEquals("CLIENT-312", response.clientId());
        assertEquals("STORE-312", response.storeId());
        assertEquals(999.0, response.finalAmount());
    }

    @Test
    void createReceipt_WithBadRequest400_ShouldReturnFallbackResponse() {
        var cash = new Cash();
        cash.setPaymentMethodType(PaymentMethodType.CASH);

        var payment = new CashPayment();
        payment.setOrderId("ORDER-313");
        payment.setClientId("CLIENT-313");
        payment.setStoreId("STORE-313");
        payment.setFinalAmount(111.0);
        payment.setPaymentMethod(cash);
        payment.setPaymentStatus(PaymentStatus.PROCESSING);

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(400)
                .setBody("bad request"));

        CreateReceiptResponse response = receiptProviderAdapter.createReceipt(payment);

        assertNotNull(response);
        assertNull(response.receiptId());
        assertEquals("ORDER-313", response.orderId());
        assertEquals("CLIENT-313", response.clientId());
        assertEquals("STORE-313", response.storeId());
        assertEquals(111.0, response.finalAmount());
    }

    @Test
    void buildUrl_WhenBaseUrlEndsWithApiAndBasePathIsApi_ShouldAvoidDuplicateApi() throws Exception {
        setField(receiptProviderAdapter, "baseUrl", "http://example.com/api/");
        setField(receiptProviderAdapter, "basePath", "/api");

        String url = invokePrivateString(receiptProviderAdapter, "buildUrl", new Class<?>[]{String.class}, "");

        assertEquals("http://example.com/api", url);
    }

    @Test
    void buildUrl_WhenBaseUrlEndsWithApiAndBasePathStartsWithApiSlash_ShouldStripApiPrefix() throws Exception {
        setField(receiptProviderAdapter, "baseUrl", "http://example.com/api");
        setField(receiptProviderAdapter, "basePath", "/api/v1/receipts/");

        String urlNoSuffix = invokePrivateString(receiptProviderAdapter, "buildUrl", new Class<?>[]{String.class}, "");
        assertEquals("http://example.com/api/v1/receipts", urlNoSuffix);

        String urlWithSuffix = invokePrivateString(receiptProviderAdapter, "buildUrl", new Class<?>[]{String.class}, "create");
        assertEquals("http://example.com/api/v1/receipts/create", urlWithSuffix);
    }

    private String invokePrivateString(Object target, String methodName, Class<?>[] paramTypes, Object... args) throws Exception {
        Method m = target.getClass().getDeclaredMethod(methodName, paramTypes);
        m.setAccessible(true);
        return (String) m.invoke(target, args);
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
