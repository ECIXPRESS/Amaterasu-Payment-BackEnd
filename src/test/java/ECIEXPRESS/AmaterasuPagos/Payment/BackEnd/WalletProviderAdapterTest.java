package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Cash;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.PaymentMethodType;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.PaymentStatus;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Wallet.WalletProviderAdapter;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Wallet.Dto.WalletResponses.PayWithWalletResponse;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Web.Dto.PaymentRequests.CreatePaymentRequest;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class WalletProviderAdapterTest {

    private MockWebServer mockWebServer;
    private WalletProviderAdapter walletProviderAdapter;

    @BeforeEach
    void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        walletProviderAdapter = new WalletProviderAdapter(new RestTemplate());
        setField(walletProviderAdapter, "baseUrl", mockWebServer.url("/").toString());
        setField(walletProviderAdapter, "basePath", "/api/v1/wallet");
    }

    @AfterEach
    void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    void processPayment_WithNullRequest_ShouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> walletProviderAdapter.processPayment(null));
    }

    @Test
    void processPayment_WithNotFound404_ShouldThrowIllegalStateException() {
        var paymentMethod = new Cash();
        paymentMethod.setPaymentMethodType(PaymentMethodType.CASH);
        var request = new CreatePaymentRequest("ORDER-220", "CLIENT-404", "STORE-1", 10.0, paymentMethod, null);

        mockWebServer.enqueue(new MockResponse().setResponseCode(404).setBody("not-found"));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> walletProviderAdapter.processPayment(request));
        assertTrue(ex.getMessage().contains("Wallet not found"));
    }

    @Test
    void processPayment_WithBadRequest400AndBody_ShouldThrowIllegalStateException_AndIncludeBody() {
        var paymentMethod = new Cash();
        paymentMethod.setPaymentMethodType(PaymentMethodType.CASH);
        var request = new CreatePaymentRequest("ORDER-221", "CLIENT-400", "STORE-1", 10.0, paymentMethod, null);

        mockWebServer.enqueue(new MockResponse().setResponseCode(400).setBody("bad-request"));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> walletProviderAdapter.processPayment(request));
        assertTrue(ex.getMessage().contains("400"));
        assertTrue(ex.getMessage().contains("bad-request"));
    }

    @Test
    void processPayment_WithServerError500WithoutBody_ShouldReturnFallbackResponse() {
        var paymentMethod = new Cash();
        paymentMethod.setPaymentMethodType(PaymentMethodType.CASH);
        var request = new CreatePaymentRequest("ORDER-222", "CLIENT-500", "STORE-1", 10.0, paymentMethod, null);

        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        PayWithWalletResponse response = walletProviderAdapter.processPayment(request);
        assertNotNull(response);
        assertNull(response.paymentStatus());
    }

    @Test
    void processPayment_WithNon2xxResponseEntity_WhenErrorHandlerDoesNotThrow_ShouldThrowIllegalStateException() throws Exception {
        RestTemplate rt = new RestTemplate();
        rt.setErrorHandler(new NoOpErrorHandler());
        WalletProviderAdapter adapter = new WalletProviderAdapter(rt);
        setField(adapter, "baseUrl", mockWebServer.url("/").toString());
        setField(adapter, "basePath", "/api/v1/wallet");

        var paymentMethod = new Cash();
        paymentMethod.setPaymentMethodType(PaymentMethodType.CASH);
        var request = new CreatePaymentRequest("ORDER-223", "CLIENT-NON2XX", "STORE-1", 10.0, paymentMethod, null);

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(400)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("{\"paymentStatus\":\"COMPLETED\"}"));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> adapter.processPayment(request));
        assertTrue(ex.getMessage().contains("non-2xx"));
    }

    @Test
    void processPayment_WithNoContent204_ShouldThrowIllegalStateExceptionEmptyBody() {
        var paymentMethod = new Cash();
        paymentMethod.setPaymentMethodType(PaymentMethodType.CASH);
        var request = new CreatePaymentRequest("ORDER-224", "CLIENT-204", "STORE-1", 10.0, paymentMethod, null);

        mockWebServer.enqueue(new MockResponse().setResponseCode(204));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> walletProviderAdapter.processPayment(request));
        assertTrue(ex.getMessage().toLowerCase().contains("empty body"));
    }

    @Test
    void processPayment_SendsExpectedPathAndBody() throws Exception {
        var paymentMethod = new Cash();
        paymentMethod.setPaymentMethodType(PaymentMethodType.CASH);
        var request = new CreatePaymentRequest("ORDER-225", "CLIENT-BODY", "STORE-1", 50000.0, paymentMethod, null);

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("{\"paymentStatus\":\"COMPLETED\"}"));

        PayWithWalletResponse response = walletProviderAdapter.processPayment(request);

        assertNotNull(response);
        assertEquals(PaymentStatus.COMPLETED, response.paymentStatus());

        RecordedRequest recorded = mockWebServer.takeRequest();
        assertEquals("/api/v1/wallet/pay", recorded.getPath());
        assertTrue(recorded.getHeader(HttpHeaders.CONTENT_TYPE).contains(MediaType.APPLICATION_JSON_VALUE));

        String body = recorded.getBody().readUtf8();
        assertTrue(body.contains("\"clientId\":\"CLIENT-BODY\""));
        assertTrue(body.contains("\"moneyAmount\":50000"));
    }

    @Test
    void toDouble_PrivateHelper_ShouldHandleNullNumberAndStringAndInvalid() throws Exception {
        assertEquals(0d, invokePrivateDouble(walletProviderAdapter, "toDouble", new Class<?>[]{Object.class}, (Object) null));

        assertEquals(12.5d, invokePrivateDouble(walletProviderAdapter, "toDouble", new Class<?>[]{Object.class}, 12.5d));

        assertEquals(42d, invokePrivateDouble(walletProviderAdapter, "toDouble", new Class<?>[]{Object.class}, "42"));

        // Update this assertion to expect InvocationTargetException and verify its cause
        Exception exception = assertThrows(InvocationTargetException.class,
                () -> invokePrivateDouble(walletProviderAdapter, "toDouble", new Class<?>[]{Object.class}, "nope"));
        assertTrue(exception.getCause() instanceof IllegalArgumentException);
        assertTrue(exception.getCause().getMessage().contains("Unable to convert amount to double: nope"));
    }

    @Test
    void normalizePath_PrivateHelper_ShouldTrimAndNormalize() throws Exception {
        String normalized = invokePrivateString(walletProviderAdapter, "normalizePath", new Class<?>[]{String.class}, "  /api/v1/wallet/  ");
        assertEquals("/api/v1/wallet", normalized);

        String blank = invokePrivateString(walletProviderAdapter, "normalizePath", new Class<?>[]{String.class}, "   ");
        assertEquals("", blank);
    }

    private double invokePrivateDouble(Object target, String methodName, Class<?>[] paramTypes, Object... args) throws Exception {
        Method m = target.getClass().getDeclaredMethod(methodName, paramTypes);
        m.setAccessible(true);
        return (double) m.invoke(target, args);
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

    private static class NoOpErrorHandler implements ResponseErrorHandler {
        @Override
        public boolean hasError(ClientHttpResponse response) throws IOException {
            return false;
        }

        @Override
        public void handleError(ClientHttpResponse response) throws IOException {
            // no-op
        }
    }
}
