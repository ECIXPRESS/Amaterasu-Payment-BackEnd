package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto.PaymentRequests.CreatePaymentRequest;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Cash;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.PaymentMethodType;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.PaymentStatus;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Wallet.WalletProviderAdapter;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Wallet.Dto.WalletResponses.CreateWalletResponse;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

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
    void processPayment_WithSuccessfulResponse_ShouldReturnWalletResponse() {
        var paymentMethod = new Cash();
        paymentMethod.setPaymentMethodType(PaymentMethodType.CASH);
        var request = new CreatePaymentRequest(
                "ORDER-200", "CLIENT-ABC", "STORE-XYZ", 50000.0, paymentMethod, null
        );

        var jsonResponse = """
        { "paymentStatus": "COMPLETED" }
        """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(jsonResponse));

        CreateWalletResponse response = walletProviderAdapter.processPayment(request);

        assertNotNull(response);
        assertEquals(PaymentStatus.COMPLETED, response.paymentStatus());
    }

    @Test
    void processPayment_WithServerError_ShouldReturnFallbackResponse() {
        var paymentMethod = new Cash();
        paymentMethod.setPaymentMethodType(PaymentMethodType.CASH);
        var request = new CreatePaymentRequest(
                "ORDER-201", "CLIENT-DEF", "STORE-UVW", 60000.0, paymentMethod, null
        );

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("error"));

        CreateWalletResponse response = walletProviderAdapter.processPayment(request);

        assertNotNull(response);
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